#!/usr/bin/env python3
"""Gerçek araç kayıtlarından devir katmanlı motor sesleri üretir.

Her motor sesi için (tools/sound_sources.py) açık lisanslı kayıtlardan üç
döngü kesilir: rölanti, orta ve yüksek devir. Oyunda bu üçü devre göre
karıştırılır ve her biri kendi kayıt devrine göre perdelenir; motor her
devirde kendine yakın gerçek bir kaydın tınısıyla duyulur.

Adımlar:
  1. Katman pencereleri kaynak tablosunda elle seçilmiştir (spektrograma
     bakılarak: rölanti, sabit tutulan gaz ya da kısa bir gaz vuruşu).
  2. Perde sabitleme. Penceredeki güçlü bir harmonik çizgi kare kare izlenir
     ve sinyal, o çizgi sabit kalacak şekilde değişken hızla yeniden
     örneklenir; gaz vuruşu bile sabit devirli bir sese dönüşür.
  3. Döngü. Uzunluk tam sayıda motor çevrimi olacak şekilde kesilir ve uçlar
     eşit güçte çapraz geçişle birleştirilir; döngü noktası duyulmaz.
  4. Devir oranı. Döngülerin log-frekans spektrumları, tablodaki kaba
     tahminin ±0.3 oktavı içinde hizalanarak ölçülür. Mutlak devir izine
     güvenilmez: harmonik yöntemler yükteki motorda yarım mertebelere kayıp
     oktav hatası yapıyor, kayıtların ses seviyesi de kamera kazancı yüzünden
     devri yansıtmıyor.

Çıktı:
  src/main/resources/assets/realcars/sounds/engine/<ses>_<katman>.ogg
  src/main/resources/assets/realcars/sounds/engine/<ses>_start.ogg / _stop.ogg  (marş, kapatma)
  src/main/resources/assets/realcars/engine_voices.json   (katman devir oranları)
  CREDITS.md                                              (atıflar)

Kullanım:
    pip install numpy scipy soundfile pyarrow huggingface_hub
    python3 tools/import_engine_sounds.py            # hepsi
    python3 tools/import_engine_sounds.py beetle     # tek ses
"""

import json
import pathlib
import sys

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import sound_sources  # noqa: E402
import spec  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".snd_cache"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / spec.MOD_ID
OUT = ASSETS / "sounds" / "engine"
PROFILE = ASSETS / "engine_voices.json"


SR = 44100
FPS = 50
LOOP_RMS = 0.18


# ---------------------------------------------------------------------------
# İndirme: Freesound kayıtları, Hugging Face'teki lisans bilgili aynadan
# ---------------------------------------------------------------------------

def download(rec_id):
    """Kaydı aynadaki parquet parçasından okur (yalnızca o satır grubunu)."""
    import pyarrow.parquet as pq
    from huggingface_hub import HfFileSystem

    CACHE.mkdir(parents=True, exist_ok=True)
    dst = CACHE / ("freesound_%d.flac" % rec_id)
    if dst.exists() and dst.stat().st_size > 1000:
        return dst
    shard, row = sound_sources.RECORDINGS[rec_id]["hf"]
    print("  indiriliyor: freesound %d (%s)" % (rec_id, sound_sources.RECORDINGS[rec_id]["title"]))
    path = "datasets/%s/data/%s" % (sound_sources.FREESOUND_MIRROR, shard)
    pf = pq.ParquetFile(HfFileSystem().open(path, "rb", block_size=1 << 20))
    start = 0
    for g in range(pf.num_row_groups):
        n = pf.metadata.row_group(g).num_rows
        if row < start + n:
            rows = pf.read_row_group(g, columns=["audio", "freesound_id"]).to_pylist()
            entry = rows[row - start]
            if entry["freesound_id"] != rec_id:
                raise RuntimeError("aynada beklenen kayıt yok: %d" % rec_id)
            dst.write_bytes(entry["audio"]["bytes"])
            return dst
        start += n
    raise RuntimeError("satır bulunamadı: %d" % rec_id)


# ---------------------------------------------------------------------------
# Ses işleme
# ---------------------------------------------------------------------------

def load(path):
    import soundfile as sf

    d, sr = sf.read(str(path), always_2d=True)
    d = d.mean(1)
    if sr != SR:
        t = np.arange(0, len(d) / sr, 1 / SR)
        d = np.interp(t, np.arange(len(d)) / sr, d)
    return d


def flatten(d, times, f, start, end, f_target):
    """[start, end) aralığını çevrim frekansı f_target'te sabit olacak şekilde
    değişken hızla yeniden örnekler."""
    i0, i1 = int(start * SR), int(end * SR)
    t = np.arange(i0, i1) / SR
    fi = np.interp(t, times, f)
    phase = np.concatenate([[0.0], np.cumsum(fi[:-1] / SR)])
    n_out = int(phase[-1] / f_target * SR)
    src_t = np.interp(np.arange(n_out) * f_target / SR, phase, t)
    return np.interp(src_t * SR, np.arange(len(d)), d)


def make_loop(x, f_target, xfade=0.35):
    """Tam sayıda çevrimlik döngü; uçlar eşit güçte çapraz geçişle birleşir."""
    period = SR / f_target
    cycles = int((len(x) / (1 + xfade)) / period)
    if cycles < 4:
        raise ValueError("segment döngü için kısa")
    length = int(round(cycles * period))
    fade = min(int(length * xfade), len(x) - length)
    loop = x[:length].copy()
    a = np.linspace(0, np.pi / 2, fade)
    loop[:fade] = x[:fade] * np.sin(a) + x[length:length + fade] * np.cos(a)
    return loop


def highpass(x, fc=25.0):
    X = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    low = fr < fc
    X[low] *= (fr[low] / fc) ** 2
    return np.fft.irfft(X, len(x))


LOG_BINS = 96  # oktav başına


def log_spectrum(S, freqs, i0, i1):
    """Pencerenin ortalama spektrumu, log-frekans ekseninde, yalnızca
    harmonik deseni kalacak şekilde düzleştirilmiş."""
    from scipy.ndimage import uniform_filter1d

    u = np.arange(np.log2(30.0), np.log2(3000.0), 1.0 / LOG_BINS)
    spec = np.log(S[i0:i1].mean(0) + 1e-9)
    a = np.interp(2.0 ** u, freqs, spec)
    a = a - uniform_filter1d(a, LOG_BINS // 3)
    return (a - a.mean()) / (a.std() + 1e-9)


def spectral_ratio(a, b, guess, octaves=(-1, 0, 1), reach=0.15):
    """b'nin a'ya göre perde oranı: log-frekans spektrumlarının en iyi
    hizalandığı kayma. Adaylar izleyicinin tahmini ve bir oktav altı/üstü
    (oktav hatası en sık hata), her biri ±0.15 oktav içinde inceltilir."""
    best = (-1e9, guess)
    for centre in (np.log2(guess) + o for o in octaves):
        for k in range(int((centre - reach) * LOG_BINS), int((centre + reach) * LOG_BINS) + 1):
            if k >= 0:
                c = np.dot(a[:len(a) - k], b[k:]) / (len(a) - k)
            else:
                c = np.dot(a[-k:], b[:len(b) + k]) / (len(b) + k)
            if c > best[0]:
                best = (c, 2.0 ** (k / LOG_BINS))
    return best[1]


def follow_line(d, t0, t1, hz, n=16384, fps=FPS):
    """Spektrogramda hz yakınındaki harmonik çizgiyi kare kare izler (her
    karede bir önceki frekansın ±%6'sı içinde en güçlü tepe, parabolik
    inceltmeyle). Bütün katmanlarda aynı mertebedeki çizgi izlendiği için
    oranlar oktav hatasından etkilenmez."""
    seg = d[int(t0 * SR):int(t1 * SR)]
    hop = SR // fps
    win = np.hanning(n)
    fr = np.fft.rfftfreq(n, 1 / SR)
    df = fr[1]
    times, track = [], []
    f = hz
    for i in range(0, max(1, len(seg) - n), hop):
        spec = np.log(np.abs(np.fft.rfft(seg[i:i + n] * win)) + 1e-9)
        # Adım adım kaymayı önlemek için başlangıç frekansının ±%35'i ile sınırlı.
        lo = int(max(f * 0.94, hz * 0.65) / df)
        hi = int(min(f * 1.06, hz * 1.35) / df) + 1
        if hi - lo < 3:
            # İz bandın kenarına dayandı: bütün bantta yeniden ara.
            lo, hi = int(hz * 0.65 / df), int(hz * 1.35 / df) + 1
        k = lo + int(np.argmax(spec[lo:hi]))
        a, b, c = spec[k - 1], spec[k], spec[k + 1]
        den = a - 2 * b + c
        off = 0.5 * (a - c) / den if den < 0 else 0.0
        f = (k + off) * df
        times.append(t0 + (i + n / 2) / SR)
        track.append(f)
    from scipy.ndimage import median_filter

    # Tek karelik sıçramalar (gürültüye kapılma) atılır, gerçek devir
    # değişimi kalır.
    return np.array(times), median_filter(np.array(track), 9, mode="nearest")


def dominant_line(d, t0, t1, lo=30.0, hi=400.0, n=16384):
    """Pencerenin ortalama spektrumundaki en belirgin harmonik tepe (Hz)."""
    from scipy.ndimage import uniform_filter1d

    seg = d[int(t0 * SR):int(t1 * SR)]
    hops = range(0, max(1, len(seg) - n), n // 4)
    spec = np.mean([np.abs(np.fft.rfft(seg[i:i + n] * np.hanning(n))) for i in hops], axis=0)
    fr = np.fft.rfftfreq(n, 1 / SR)
    level = np.log(spec + 1e-9)
    prominence = level - uniform_filter1d(level, 40)
    band = (fr >= lo) & (fr <= hi)
    return float(fr[band][np.argmax(prominence[band])])


def loop_spectrum(x):
    """Döngünün log-frekans ekseninde düzleştirilmiş ortalama spektrumu."""
    n = 16384
    # Döngü periyodik olduğu için ucuna eklenerek uzatılabilir.
    x = np.tile(x, int(np.ceil(3 * n / len(x))))
    hops = range(0, len(x) - n, n // 4)
    S = np.array([np.abs(np.fft.rfft(x[i:i + n] * np.hanning(n))) for i in hops])
    return log_spectrum(S, np.fft.rfftfreq(n, 1 / SR), 0, len(S))


def resample(x, factor):
    """Perdeyi factor kadar yükseltir (süre 1/factor olur)."""
    return np.interp(np.arange(0, len(x) - 1, factor), np.arange(len(x)), x)


def even_envelope(loop, window=0.08, strength=0.7):
    """Döngü içindeki ses yüksekliği dalgalanmasını yumuşatır. Kısa bir
    döngüde her tekrarda duyulan "vuv vuv" kabarması sesi makine gibi
    gösterir; zarf ~80 ms ölçeğinde kısmen düzleştirilir, ateşleme
    darbeleri (daha kısa ölçek) korunur. Hesap döngünün kendisi üzerinde
    dairesel yapılır ki dikiş bozulmasın."""
    from scipy.ndimage import uniform_filter1d

    n = len(loop)
    tiled = np.tile(loop, 3)
    env = np.sqrt(uniform_filter1d(tiled ** 2, int(window * SR)) + 1e-12)[n:2 * n]
    gain = np.clip((env.mean() / env) ** strength, 0.5, 2.0)
    return loop * gain


def cut(spec, fade_out):
    d = load(download(spec["rec"]))
    t0, t1 = spec["t"]
    x = highpass(d[int(t0 * SR):int(t1 * SR)].copy())
    fi = int(0.008 * SR)
    x[:fi] *= np.linspace(0, 1, fi)
    fo = min(int(fade_out * SR), len(x) // 2)
    x[-fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    return x


def run_down(idle_loop, seconds=1.3):
    """Motor kapanırken devir düşer, ateşlemeler seyrekleşip söner: rölanti
    döngüsü giderek yavaşlayan hızla çalınır ve sönümlenir."""
    base = np.tile(idle_loop, int(np.ceil(3 * SR / len(idle_loop))))
    n = int(seconds * SR)
    t = np.arange(n) / n
    rate = 1.0 - 0.68 * t ** 1.4            # devir rölantiden ~%30'a düşer
    pos = np.cumsum(rate)
    x = np.interp(pos, np.arange(len(base)), base)
    env = (1.0 - t) ** 1.6 * np.minimum(1.0, rate / 0.6)
    fi = int(0.01 * SR)
    env[:fi] *= np.linspace(0, 1, fi)
    return x * env


def write_event(name, x, peak_rms):
    """En gür 0.2 sn'nin RMS'i peak_rms olacak şekilde ölçekleyip yazar."""
    import soundfile as sf
    from scipy.ndimage import uniform_filter1d

    loud = np.sqrt(uniform_filter1d(x ** 2, int(0.2 * SR)).max() + 1e-12)
    x = np.clip(x / loud * peak_rms, -0.98, 0.98)
    sf.write(str(OUT / (name + ".ogg")), x.astype(np.float32), SR, format="OGG", subtype="VORBIS")


def convert(voice, src):
    """Her katmanın kaydı ve saniye aralığı kaynak tablosunda elle seçilir
    (spektrograma bakılarak); devir oranı için kaba bir tahmin verilir.

    Katman penceresindeki en belirgin harmonik çizgi kare kare izlenir ve
    perde onun ortalamasında sabitlenir; böylece kısa bir gaz vuruşu bile
    sabit devirli bir döngüye dönüşür. Oran, döngülerin log-frekans
    spektrumları tahminin ±0.15 oktavı içinde hizalanarak ölçülür. Dar arama
    oktav hatasını imkânsız kılar; devir izinin mutlak değerine ise hiç
    güvenilmez (yükteki motorda harmonik tarak yarım mertebelere kayıyor,
    kayıtların ses seviyesi de kamera kazancı yüzünden devri yansıtmıyor)."""
    import soundfile as sf

    OUT.mkdir(parents=True, exist_ok=True)
    loops, report = {}, {}
    for layer in ("idle", "mid", "high"):
        spec = src["layers"][layer]
        d = load(download(spec["rec"]))
        t0, t1 = spec["t"]
        hz = spec.get("hz") or dominant_line(d, t0, t1)
        times, line = follow_line(d, t0, t1, hz)
        fm = float(np.exp(np.log(line).mean()))
        x = flatten(d, times, line, times[0], times[-1], fm)
        shift = spec.get("shift", 1.0)
        if shift != 1.0:
            # Kayıtta bu devre çıkılmamış: orta katmanın kaydı perdesi
            # kaydırılarak kullanılır (en fazla 1.5 kat).
            x = resample(x, shift)
            fm *= shift
        loops[layer] = highpass(make_loop(x, fm))
        report[layer] = dict(hz=round(fm, 1), swing=round(float(np.log(line.max() / line.min())), 2),
                             seconds=round(len(loops[layer]) / SR, 2))

    ref = loop_spectrum(loops["idle"])
    ratios = {}
    for layer in ("mid", "high"):
        prior = src["ratios"][layer]
        ratios[layer] = spectral_ratio(ref, loop_spectrum(loops[layer]), prior, octaves=(0,), reach=0.3)
    if not (ratios["mid"] > 1.08 and ratios["high"] > ratios["mid"] * 1.08):
        raise RuntimeError("%s: katmanlar devirde sıralı değil %s" % (voice, ratios))
    for layer, loop in loops.items():
        loop = even_envelope(loop)
        loop = np.clip(loop / (np.sqrt(np.mean(loop ** 2)) + 1e-9) * LOOP_RMS, -0.98, 0.98)
        sf.write(str(OUT / ("%s_%s.ogg" % (voice, layer))), loop.astype(np.float32), SR,
                 format="OGG", subtype="VORBIS")

    # Marş: kaydın kendisi (marş motoru, ilk ateşleme, devrin parlayıp
    # oturması). Oyunda rölanti döngüsü bunun altında açılır, bu yüzden son
    # yarım saniyede yumuşakça söner.
    start = cut(src["start"], fade_out=0.6)
    write_event(voice + "_start", start, peak_rms=0.32)
    # Kapatma: gerçek kayıt varsa o, yoksa aracın kendi rölantisi devri
    # düşerek sönen motor olarak.
    if src.get("stop"):
        stop = cut(src["stop"], fade_out=0.3)
    else:
        stop = run_down(loops["idle"])
    write_event(voice + "_stop", stop, peak_rms=0.24)
    print("  %-12s orta ×%.2f (tahmin %.2f), yüksek ×%.2f (tahmin %.2f)  %s" % (
        voice, ratios["mid"], src["ratios"]["mid"], ratios["high"], src["ratios"]["high"], report))
    return dict(mid=round(ratios["mid"], 3), high=round(ratios["high"], 3))


def main(argv):
    import import_hd_models

    names = [a for a in argv if not a.startswith("--")] or list(sound_sources.VOICES)
    profile = json.loads(PROFILE.read_text()) if PROFILE.exists() else {}
    for voice in names:
        profile[voice] = convert(voice, sound_sources.VOICES[voice])
    PROFILE.write_text(json.dumps({k: profile[k] for k in sorted(profile)}, indent=1) + "\n")
    print("katman oranları ->", PROFILE.relative_to(ROOT))
    import_hd_models.write_credits()


def credits_section():
    """CREDITS.md için motor sesleri bölümü (import_hd_models.write_credits çağırır)."""
    lic_urls = {"CC0-1.0": "https://creativecommons.org/publicdomain/zero/1.0/",
                "CC-BY-3.0": "https://creativecommons.org/licenses/by/3.0/",
                "CC-BY-4.0": "https://creativecommons.org/licenses/by/4.0/"}
    used = {}
    for voice, cfg in sound_sources.VOICES.items():
        parts = list(cfg["layers"].values()) + [cfg["start"]] + ([cfg["stop"]] if cfg.get("stop") else [])
        for part in parts:
            used.setdefault(part["rec"], []).append(voice)
    lines = [
        "",
        "## Motor sesleri",
        "",
        "Motor sesleri aşağıdaki gerçek kayıtlardan üretilmiştir (Freesound; Hugging Face'teki",
        "[lisans bilgili aynasından](https://huggingface.co/datasets/%s) indirildi)." % sound_sources.FREESOUND_MIRROR,
        "Her kayıttan rölanti, orta ve yüksek devir döngüleri kesildi, perdesi harmonik çizgi",
        "izlenerek sabitlendi ve döngü noktası çapraz geçişle birleştirildi; marş sesleri de",
        "kayıtların kendi marş anlarından kesildi",
        "(`tools/import_engine_sounds.py`).",
        "",
        "| Ses | Kayıt | Yazar | Lisans |",
        "|---|---|---|---|",
    ]
    for rec, voices in used.items():
        r = sound_sources.RECORDINGS[rec]
        lines.append("| %s | [%s](https://freesound.org/s/%d/) | %s | [%s](%s) |" % (
            ", ".join("`%s`" % v for v in sorted(set(voices))), r["title"].replace("|", "/"), rec,
            r["author"], r["license"], lic_urls[r["license"]]))
    return lines


if __name__ == "__main__":
    main(sys.argv[1:])
