#!/usr/bin/env python3
"""RealCars ses üreticisi.

Motor sesleri spektral (FFT) sentezle üretilir: dalga biçimi doğrudan frekans
alanında kurulur ve ters FFT ile zamana çevrilir. Bu yöntem üretilen tamponun
tam olarak periyodik olmasını garanti eder, yani döngü noktasında hiçbir "tık"
duyulmaz — motor sesi kesintisiz döner.

Motor sesi tek bir referans devirde (REF_RPM) üretilir; oyun içinde gerçek
devre göre perdesi (pitch) değiştirilerek 800-7500 d/dk aralığı kaplanır.

Çıktı: src/main/resources/assets/realcars/sounds/*.ogg
"""

import pathlib
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import spec  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
SND = ROOT / "src/main/resources/assets" / spec.MOD_ID / "sounds"

SR = 44100
REF_RPM = 3000.0   # motor döngüsünün üretildiği referans devir

# id -> (silindir, sertlik, egzoz gürültüsü, yarım-mertebe içeriği)
#   sertlik          : yüksek harmoniklerin gücü (tiz/agresiflik)
#   egzoz gürültüsü  : bant sınırlı gürültünün seviyesi
#   yarım-mertebe    : V8'lerin karakteristik "burble" sesini veren, ateşleme
#                      mertebesinin yarısındaki bileşenlerin gücü
ENGINE_VOICES = {
    "engine_loop_i4":    dict(cyl=4, harsh=0.72, noise=0.16, halforder=0.06),
    "engine_loop_i6":    dict(cyl=6, harsh=0.52, noise=0.11, halforder=0.04),
    "engine_loop_v6":    dict(cyl=6, harsh=0.66, noise=0.15, halforder=0.14),
    "engine_loop_v8":    dict(cyl=8, harsh=0.80, noise=0.20, halforder=0.34),
    "engine_loop_flat6": dict(cyl=6, harsh=0.62, noise=0.18, halforder=0.10),
}


# --------------------------------------------------------------------------
# Yardımcılar
# --------------------------------------------------------------------------

def _write(name, data, normalize=0.86, target_rms=None):
    """OGG olarak yaz.

    Varsayılan olarak tepe değere göre normalleştirir. `target_rms` verilirse
    önce ortalama güce göre ölçekler — tek tük yüksek darbeler içeren (çakıl
    gibi) seslerin genel seviyesinin çökmesini engeller.
    """
    data = np.asarray(data, dtype=np.float64)
    if target_rms is not None:
        rms = float(np.sqrt(np.mean(data ** 2)))
        if rms > 0:
            data = _soft_clip(data / rms * target_rms, 1.1)
    peak = np.max(np.abs(data))
    if peak > 0:
        data = data / peak * normalize
    SND.mkdir(parents=True, exist_ok=True)
    sf.write(SND / f"{name}.ogg", data.astype(np.float32), SR, format="OGG", subtype="VORBIS")
    return len(data) / SR


def _soft_clip(x, drive=1.0):
    return np.tanh(x * drive) / np.tanh(drive)


def _fade(x, attack=0.005, release=0.02):
    n = len(x)
    a = min(int(attack * SR), n // 2)
    r = min(int(release * SR), n // 2)
    if a > 0:
        x[:a] *= np.linspace(0.0, 1.0, a)
    if r > 0:
        x[-r:] *= np.linspace(1.0, 0.0, r)
    return x


def _rng(seed):
    return np.random.default_rng(seed)


def _band_noise(n, lo, hi, seed, tilt=0.0):
    """Bant sınırlı, periyodik (döngülenebilir) gürültü."""
    rng = _rng(seed)
    nf = n // 2 + 1
    freqs = np.fft.rfftfreq(n, 1 / SR)
    mag = np.zeros(nf)
    band = (freqs >= lo) & (freqs <= hi)
    mag[band] = 1.0
    # yumuşak kenarlar
    mag = np.convolve(mag, np.ones(9) / 9, mode="same")
    with np.errstate(divide="ignore"):
        shape = np.where(freqs > 0, freqs, 1.0) ** tilt
    mag = mag * shape
    phase = rng.uniform(0, 2 * np.pi, nf)
    spec_ = mag * np.exp(1j * phase)
    spec_[0] = 0
    return np.fft.irfft(spec_, n)


# --------------------------------------------------------------------------
# Motor döngüsü
# --------------------------------------------------------------------------

def engine_loop(voice, seconds=2.0, seed=0):
    """Kusursuz döngülenen motor sesi üretir.

    Krank frekansının yarısı (f_half) taban alınır; böylece hem tam mertebeler
    (ateşleme darbeleri) hem de yarım mertebeler (V motorların burble sesi)
    tamsayı FFT bin'lerine denk gelir ve tampon tam periyodik olur.
    """
    n = int(seconds * SR)
    n -= n % 2
    f_crank = REF_RPM / 60.0                 # d/dk -> Hz
    f_half = f_crank / 2.0
    bin_hz = SR / n
    # f_half'i tam bir bin'e oturt ki tüm katları da tam bin olsun
    k_half = max(1, int(round(f_half / bin_hz)))
    f_half = k_half * bin_hz

    order_firing = voice["cyl"] / 2.0        # 4 zamanlı: devir başına ateşleme
    nf = n // 2 + 1
    mag = np.zeros(nf)
    rng = _rng(seed)
    phase = rng.uniform(0, 2 * np.pi, nf)

    for m in range(1, 220):                  # f_half'in katları
        k = m * k_half
        if k >= nf:
            break
        order = m / 2.0                      # krank mertebesi
        # temel sönüm
        amp = 1.0 / (order ** (1.55 - 0.75 * voice["harsh"]))
        # ateşleme mertebesi ve katları öne çıkar
        if abs(order / order_firing - round(order / order_firing)) < 1e-6:
            amp *= 3.4
        elif abs(m % 2) > 0:                 # yarım mertebeler (burble)
            amp *= voice["halforder"]
        else:
            amp *= 0.45
        # tiz bölgede yumuşak kesim (rezonatör/susturucu etkisi)
        f = k * bin_hz
        amp *= 1.0 / (1.0 + (f / (900.0 + 2600.0 * voice["harsh"])) ** 2)
        mag[k] = amp

    spec_ = mag * np.exp(1j * phase)
    spec_[0] = 0.0
    tone = np.fft.irfft(spec_, n)
    tone /= np.max(np.abs(tone)) or 1.0

    # Emme/egzoz gürültüsü: ateşleme darbeleriyle genlik modülasyonu
    noise = _band_noise(n, 180, 5200, seed + 17, tilt=-0.35)
    noise /= np.max(np.abs(noise)) or 1.0
    t = np.arange(n) / SR
    pulse = 0.55 + 0.45 * np.abs(np.sin(np.pi * f_crank * order_firing * t))
    sig = tone + voice["noise"] * noise * pulse

    sig = _soft_clip(sig, 1.5 + 1.4 * voice["harsh"])
    return sig


# --------------------------------------------------------------------------
# Diğer sesler
# --------------------------------------------------------------------------

def engine_start(seconds=1.4):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)

    crank_end = int(0.85 * SR)
    tc = t[:crank_end]
    # marş motoru: ~11 Hz'de darbeli, tiz bir vınlama
    whine = np.sin(2 * np.pi * 1250 * tc) * 0.35 + np.sin(2 * np.pi * 640 * tc) * 0.25
    chug = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 11 * tc))
    grind = _band_noise(crank_end, 300, 4000, 91) * 0.5
    out[:crank_end] = (whine + grind) * chug * np.linspace(0.6, 1.0, crank_end)

    # ateşleme: devir hızla yükselip rölantiye oturur
    fire = np.arange(crank_end, n)
    tf = (fire - crank_end) / SR
    rpm = 600 + 2400 * np.exp(-tf * 4.2) * (1 - np.exp(-tf * 26))
    ph = 2 * np.pi * np.cumsum(rpm / 60.0 * 2.0) / SR
    burst = (np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.3 * np.sin(3 * ph))
    burst += _band_noise(len(fire), 150, 3500, 92) * 0.4
    out[crank_end:] = burst * np.minimum(1.0, tf * 12) * np.exp(-tf * 0.8)
    return _fade(_soft_clip(out, 1.8), 0.01, 0.06)


def engine_stop(seconds=0.9):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    rpm = 900 * np.exp(-t * 3.6)
    ph = 2 * np.pi * np.cumsum(rpm / 60.0 * 2.0) / SR
    sig = (np.sin(ph) + 0.45 * np.sin(2 * ph)) * np.exp(-t * 2.4)
    sig += _band_noise(n, 120, 2200, 93) * 0.25 * np.exp(-t * 5.0)
    return _fade(_soft_clip(sig, 1.4), 0.005, 0.15)


def horn(seconds=0.7):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    sig = np.zeros(n)
    # iki tonlu korna (büyük üçlü aralık), zengin harmonikli
    for f, w in ((410.0, 1.0), (512.0, 0.85)):
        for k in range(1, 9):
            sig += w * np.sin(2 * np.pi * f * k * t) / (k ** 1.25)
    env = np.minimum(1.0, t * 60) * np.minimum(1.0, (seconds - t) * 26)
    return _fade(_soft_clip(sig * env, 1.6), 0.004, 0.03)


def brake_squeal(seconds=0.8):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    wob = 1.0 + 0.035 * np.sin(2 * np.pi * 23 * t)
    sig = np.sin(2 * np.pi * 2750 * t * wob) * 0.6
    sig += np.sin(2 * np.pi * 4130 * t * wob) * 0.3
    sig += _band_noise(n, 1500, 7000, 94) * 0.35
    env = np.minimum(1.0, t * 14) * np.exp(-np.maximum(0.0, t - 0.45) * 9)
    return _fade(sig * env, 0.02, 0.08)


def gravel_loop(seconds=1.5):
    """Toprak/çakıl üzerinde sürüş — döngülenebilir."""
    n = int(seconds * SR)
    n -= n % 2
    sig = _band_noise(n, 250, 6500, 95, tilt=-0.5) * 0.9
    # tek tük taş sıçramaları (döngü sınırından uzakta tutulur)
    rng = _rng(96)
    for _ in range(26):
        p = rng.integers(int(0.05 * SR), n - int(0.12 * SR))
        ln = int(rng.uniform(0.004, 0.02) * SR)
        click = _band_noise(ln * 2, 900, 9000, int(rng.integers(1, 1 << 30)))[:ln]
        sig[p:p + ln] += click * np.exp(-np.linspace(0, 6, ln)) * rng.uniform(0.12, 0.3)
    return _soft_clip(sig, 1.2)


def crash(seconds=0.9):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    sig = _band_noise(n, 60, 9000, 97) * np.exp(-t * 9.0) * 1.2
    # metal rezonanslar
    for f, d in ((196.0, 3.0), (317.0, 4.5), (523.0, 6.0), (860.0, 8.0), (1490.0, 11.0)):
        sig += np.sin(2 * np.pi * f * t) * np.exp(-t * d) * 0.35
    # cam kırılması
    rng = _rng(98)
    for _ in range(18):
        p = rng.integers(int(0.02 * SR), int(0.5 * SR))
        ln = int(0.02 * SR)
        sig[p:p + ln] += _band_noise(ln * 2, 3000, 12000, int(rng.integers(1, 1 << 30)))[:ln] \
            * np.exp(-np.linspace(0, 8, ln)) * 0.5
    return _fade(_soft_clip(sig, 1.5), 0.001, 0.12)


def door_close(seconds=0.5):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    thud = (np.sin(2 * np.pi * 88 * t) + 0.6 * np.sin(2 * np.pi * 132 * t)) * np.exp(-t * 22)
    body = _band_noise(n, 80, 900, 99) * np.exp(-t * 26) * 0.8
    latch = np.zeros(n)
    p = int(0.055 * SR)
    ln = int(0.03 * SR)
    latch[p:p + ln] = _band_noise(ln * 2, 1800, 9000, 100)[:ln] * np.exp(-np.linspace(0, 7, ln))
    return _fade(_soft_clip(thud + body + latch * 0.7, 1.3), 0.001, 0.06)


def wrench_use(seconds=0.6):
    n = int(seconds * SR)
    sig = np.zeros(n)
    rng = _rng(101)
    for i in range(7):
        p = int((0.03 + i * 0.075) * SR)
        ln = int(0.035 * SR)
        if p + ln >= n:
            break
        click = _band_noise(ln * 2, 1200, 11000, 110 + i)[:ln]
        for f in (2100.0, 3400.0):
            click += np.sin(2 * np.pi * f * np.arange(ln) / SR) * 0.4
        sig[p:p + ln] += click * np.exp(-np.linspace(0, 9, ln)) * rng.uniform(0.7, 1.0)
    return _fade(_soft_clip(sig, 1.2), 0.001, 0.05)


def idle_loop(seconds=2.0):
    """Rölanti: düşük devirli, düzensiz, sakin bir motor sesi."""
    voice = dict(cyl=4, harsh=0.30, noise=0.26, halforder=0.18)
    return engine_loop(voice, seconds, seed=5) * 0.8


# --------------------------------------------------------------------------

def main():
    made = {}
    for name, voice in ENGINE_VOICES.items():
        secs = spec.SOUNDS[name][1]
        made[name] = _write(name, engine_loop(voice, secs, seed=hash(name) % 9999))
    made["engine_idle"] = _write("engine_idle", idle_loop(spec.SOUNDS["engine_idle"][1]))
    made["engine_start"] = _write("engine_start", engine_start(spec.SOUNDS["engine_start"][1]))
    made["engine_stop"] = _write("engine_stop", engine_stop(spec.SOUNDS["engine_stop"][1]))
    made["horn"] = _write("horn", horn(spec.SOUNDS["horn"][1]))
    made["brake_squeal"] = _write("brake_squeal", brake_squeal(spec.SOUNDS["brake_squeal"][1]))
    # Vorbis kodlaması tepe değeri bir miktar aşabildiği için çakıl döngüsünde
    # kırpılmayı önlemek adına daha düşük bir tepe hedefi kullanılır.
    made["gravel_loop"] = _write("gravel_loop", gravel_loop(spec.SOUNDS["gravel_loop"][1]),
                                 normalize=0.60, target_rms=0.34)
    made["crash"] = _write("crash", crash(spec.SOUNDS["crash"][1]))
    made["door_close"] = _write("door_close", door_close(spec.SOUNDS["door_close"][1]))
    made["wrench_use"] = _write("wrench_use", wrench_use(spec.SOUNDS["wrench_use"][1]))

    missing = set(spec.SOUNDS) - set(made)
    if missing:
        raise SystemExit(f"Üretilmeyen sesler: {sorted(missing)}")
    for k in sorted(made):
        print(f"  {k:<20} {made[k]:.2f} sn")
    print(f"{len(made)} ses üretildi -> {SND}")


if __name__ == "__main__":
    main()
