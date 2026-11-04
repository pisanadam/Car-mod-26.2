"""Gerçek motor kayıtlarının kaynak tablosu.

Kayıtlar Freesound'da açık lisansla (CC0 / CC BY) yayımlanmış ses
dosyalarıdır; dönüştürücü onları Freesound'un Hugging Face'teki lisans
bilgili aynasından (benjamin-paine/freesound-laion-640k) indirir.

RECORDINGS: freesound kimliği -> başlık, yazar, lisans ve aynadaki konum
  (parquet parçası, satır).

VOICES: her motor sesi için üç katman. Katman başına
  rec   : kayıt (RECORDINGS anahtarı)
  t     : kullanılacak saniye aralığı (spektrograma bakılarak seçildi:
          rölanti, sabit tutulan gaz ya da kısa bir gaz vuruşu)
  shift : (isteğe bağlı) kayıtta bu devre çıkılmamışsa perdenin kaç kat
          kaydırılacağı (en fazla 1.5)
  ratios: katmanların rölantiye göre kaba devir oranı tahmini; dönüştürücü
          bunu spektrum hizalamasıyla ±0.3 oktav içinde inceltir.
  note  : kayıttaki araç modun aracıyla birebir değilse neden seçildiği
  start : marş sesi (kayıt ve saniye aralığı: marş motoru, ilk ateşleme,
          devrin parlayıp rölantiye oturması)
  stop  : (isteğe bağlı) gerçek kapatma kaydı; yoksa kapatma sesi aracın
          kendi rölanti döngüsünden, devri düşerek sönen motor olarak üretilir
"""

FREESOUND_MIRROR = "benjamin-paine/freesound-laion-640k"

RECORDINGS = {
    # --- marş / kapatma kayıtları ---
    401558: dict(title="SFX Car Engine Outside Start", author="GiocoSound", license="CC0-1.0",
                 hf=("train-00630-of-01352.parquet", 294)),
    401557: dict(title="SFX Car Engine Outside Stop", author="GiocoSound", license="CC0-1.0",
                 hf=("train-00071-of-01352.parquet", 201)),
    113200: dict(title="Ignition Porsche growl", author="victormalloy", license="CC0-1.0",
                 hf=("train-00676-of-01352.parquet", 225)),
    113217: dict(title="Porsche off growl", author="victormalloy", license="CC0-1.0",
                 hf=("train-01158-of-01352.parquet", 102)),
    579211: dict(title="1970 VW Bug Engine Starting Sputtering", author="bowlingballout", license="CC0-1.0",
                 hf=("train-00462-of-01352.parquet", 241)),
    264870: dict(title="diesel starting and idling", author="paulmessier", license="CC-BY-4.0",
                 hf=("train-00561-of-01352.parquet", 307)),
    455925: dict(title="Starting of Ford V8 5 Liter engine", author="noiseloop", license="CC-BY-3.0",
                 hf=("test-00084-of-00123.parquet", 326)),
    275403: dict(title="Volkswagen Type 2 1 starting", author="nhaudio", license="CC-BY-4.0",
                 hf=("test-00040-of-00123.parquet", 386)),
    401552: dict(title="SFX Car Engine Outside Idle", author="GiocoSound", license="CC0-1.0",
                 hf=("train-01288-of-01352.parquet", 219)),
    401555: dict(title="SFX Car Engine Outside RPMMed", author="GiocoSound", license="CC0-1.0",
                 hf=("train-00362-of-01352.parquet", 224)),
    401551: dict(title="SFX Car Engine Outside RPMHigh", author="GiocoSound", license="CC0-1.0",
                 hf=("train-01334-of-01352.parquet", 257)),
    415276: dict(title="BMWM3 01", author="ikbenraar", license="CC-BY-4.0",
                 hf=("train-00074-of-01352.parquet", 221)),
    509628: dict(title="4A-GE Engine Starting and Loop", author="qopsinonstudios", license="CC-BY-4.0",
                 hf=("train-01165-of-01352.parquet", 70)),
    509626: dict(title="4A-GE Engine Loop 1", author="qopsinonstudios", license="CC0-1.0",
                 hf=("train-00069-of-01352.parquet", 330)),
    509627: dict(title="4A-GE Engine Loop 2", author="qopsinonstudios", license="CC0-1.0",
                 hf=("train-00279-of-01352.parquet", 224)),
    384058: dict(title="ford mustang v8 rear 2", author="pastori666", license="CC-BY-3.0",
                 hf=("train-00290-of-01352.parquet", 307)),
    113216: dict(title="Porsche idle", author="victormalloy", license="CC0-1.0",
                 hf=("train-01222-of-01352.parquet", 311)),
    113214: dict(title="Porsche Exhaust", author="victormalloy", license="CC0-1.0",
                 hf=("train-00936-of-01352.parquet", 156)),
    253439: dict(title="Jeep in the Mountians #1", author="deleted_user_4401185", license="CC-BY-3.0",
                 hf=("train-00342-of-01352.parquet", 154)),
    443319: dict(title="Diesel Start-Idle-Revs MZ000004", author="DAVESTALKER", license="CC0-1.0",
                 hf=("train-00286-of-01352.parquet", 9)),
    425845: dict(title="Renault Master F3500 dCi135 Engine Start Up Idle Gas Pressing And Shut Down Mono",
                 author="Soundholder", license="CC-BY-3.0", hf=("train-00038-of-01352.parquet", 179)),
    544344: dict(title="Volkswagen Beetle 1961", author="klankbeeld", license="CC-BY-4.0",
                 hf=("train-01227-of-01352.parquet", 218)),
    123685: dict(title="Chev 350 rev and idle", author="lonemonk", license="CC-BY-3.0",
                 hf=("train-00131-of-01352.parquet", 158)),
    238847: dict(title="1953 Studebaker Idle up to 6000rpm", author="lonemonk", license="CC-BY-3.0",
                 hf=("train-00278-of-01352.parquet", 238)),
    332695: dict(title="Volkswagen Type 2 revving up2", author="nhaudio", license="CC-BY-4.0",
                 hf=("train-00131-of-01352.parquet", 161)),
    249720: dict(title="Vitara V6 engine", author="Keith Selmes", license="CC-BY-3.0",
                 hf=("test-00010-of-00123.parquet", 403)),
}


def _l(rec, t0, t1, shift=1.0, hz=None):
    """hz: izlenecek harmonik çizginin yaklaşık frekansı; verilmezse pencerenin
    en belirgin tepesi kullanılır."""
    return dict(rec=rec, t=(t0, t1), shift=shift, hz=hz)


VOICES = {
    "golf_gti": dict(
        start=dict(rec=401558, t=(0.0, 2.5)),
        stop=dict(rec=401557, t=(0.0, 2.0)),
        layers=dict(idle=_l(401552, 0.2, 2.9), mid=_l(401555, 0.2, 2.6), high=_l(401551, 0.2, 1.8)),
        ratios=dict(mid=2.0, high=3.0),
        note="Oyun için kaydedilmiş benzinli bir otomobilin rölanti/orta/yüksek devir seti"),
    "corolla": dict(
        start=dict(rec=509628, t=(0.0, 2.6)),
        layers=dict(idle=_l(509628, 2.5, 7.0), mid=_l(509626, 0.2, 2.2), high=_l(509627, 2.4, 4.5)),
        ratios=dict(mid=1.8, high=2.3),
        note="AE86'nın kendi motoru: Toyota 4A-GE"),
    "bmw_m3": dict(
        start=dict(rec=415276, t=(0.8, 4.0)),
        layers=dict(idle=_l(415276, 36.0, 37.6), mid=_l(415276, 77.0, 78.5), high=_l(415276, 79.0, 80.0)),
        ratios=dict(mid=2.0, high=2.8),
        note="Gerçek bir BMW M3 kaydı"),
    "mustang_gt": dict(
        start=dict(rec=455925, t=(0.0, 4.0)),
        layers=dict(idle=_l(384058, 0.5, 4.0), mid=_l(384058, 5.2, 9.2), high=_l(384058, 5.2, 9.2, 1.5)),
        ratios=dict(mid=2.0, high=3.0),
        note="Ford Mustang V8"),
    "gtr": dict(
        start=dict(rec=238847, t=(0.0, 3.5)),
        layers=dict(idle=_l(238847, 3.5, 6.0, hz=100), mid=_l(238847, 16.0, 18.0), high=_l(238847, 29.0, 32.0)),
        ratios=dict(mid=1.65, high=2.7),
        note="Skyline'ın RB26'sının açık lisanslı kaydı bulunamadı; rölantiden 6000 d/dk'ya "
             "yavaşça çıkan 1953 Studebaker kaydı, katmanları en temiz verdiği için seçildi"),
    "porsche_911": dict(
        start=dict(rec=113200, t=(0.0, 1.1)),
        stop=dict(rec=113217, t=(0.0, 3.8)),
        layers=dict(idle=_l(113216, 3.0, 14.0), mid=_l(113214, 29.3, 31.3), high=_l(113214, 27.0, 28.3)),
        ratios=dict(mid=1.78, high=3.1),
        note="Porsche boksör 6"),
    "wrangler": dict(
        start=dict(rec=249720, t=(0.4, 3.6)),
        layers=dict(idle=_l(253439, 62.0, 68.0), mid=_l(253439, 5.0, 12.0), high=_l(253439, 5.0, 12.0, 1.5)),
        ratios=dict(mid=1.55, high=2.3),
        note="Dağ yolunda bir Jeep"),
    "hilux": dict(
        start=dict(rec=443319, t=(0.0, 3.5)),
        layers=dict(idle=_l(443319, 3.0, 10.5), mid=_l(443319, 11.3, 12.3), high=_l(443319, 11.3, 12.3, 1.5)),
        ratios=dict(mid=1.8, high=2.7),
        note="Turbo dizel: Hilux'un D-4D'sine en yakın açık lisanslı kayıt"),
    "defender": dict(
        start=dict(rec=264870, t=(1.4, 5.0)),
        layers=dict(idle=_l(425845, 5.0, 11.5, hz=78), mid=_l(425845, 12.0, 13.0, hz=86),
                    high=_l(425845, 12.0, 13.0, 1.5, hz=86)),
        ratios=dict(mid=1.8, high=2.7),
        note="Ağır ticari turbo dizel (Defender'ın dizel sürümlerine yakın)"),
    "beetle": dict(
        start=dict(rec=579211, t=(19.5, 22.8)),
        layers=dict(idle=_l(544344, 2.0, 8.0, hz=73), mid=_l(544344, 17.5, 19.5), high=_l(544344, 17.5, 19.5, 1.5)),
        ratios=dict(mid=2.0, high=3.0),
        note="1961 Volkswagen Beetle, hava soğutmalı boksör 4"),
    "c10": dict(
        start=dict(rec=123685, t=(0.0, 4.0)),
        layers=dict(idle=_l(123685, 4.0, 9.0, hz=57), mid=_l(123685, 0.9, 2.0, hz=75),
                    high=_l(123685, 0.9, 2.0, 1.5, hz=75)),
        ratios=dict(mid=2.0, high=3.0),
        note="Chevrolet 350 küçük blok V8"),
    "transporter": dict(
        start=dict(rec=275403, t=(0.0, 3.5)),
        layers=dict(idle=_l(332695, 38.0, 42.0, hz=70), mid=_l(332695, 16.0, 24.0), high=_l(332695, 29.5, 32.5, 1.5)),
        ratios=dict(mid=1.18, high=2.0),
        note="Volkswagen Type 2 (T1/T2), hava soğutmalı boksör 4"),
    "v6": dict(
        start=dict(rec=249720, t=(0.4, 3.6)),
        layers=dict(idle=_l(249720, 3.0, 9.0, hz=65), mid=_l(249720, 17.0, 22.5, hz=112),
                    high=_l(249720, 17.0, 22.5, 1.5, hz=112)),
        ratios=dict(mid=1.9, high=2.85),
        note="Modifiyeyle V6 takılan araçlar için: Suzuki Vitara V6"),
}
