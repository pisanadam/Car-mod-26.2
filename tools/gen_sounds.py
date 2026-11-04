#!/usr/bin/env python3
"""RealCars ses üreticisi: korna, fren, çarpışma, kapı, anahtar ve toprak sesleri.

Motor sesleri (döngüler, marş, kapatma) gerçek kayıtlardan gelir:
tools/import_engine_sounds.py. Buradaki sesler spektral (FFT) sentezle
üretilir; döngülerde tampon tam periyodik olduğu için dikişte tık duyulmaz.

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


def main():
    made = {}
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
