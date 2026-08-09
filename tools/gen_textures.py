#!/usr/bin/env python3
"""RealCars doku üreticisi.

Araç gövde atlası, item ikonları, blok dokuları, HUD kadranı ve GUI arka
planlarını üretir. Çıktı: src/main/resources/assets/realcars/textures/**
"""

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))

from pixel import Canvas, shade, mix  # noqa: E402
import spec  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets" / spec.MOD_ID / "textures"

# ==========================================================================
# Araç gövde atlası — ATLAS_REGIONS Java tarafındaki CarTexture ile aynıdır
# ==========================================================================
# ad -> (u, v, genişlik, yükseklik). Her bölge kendi içinde düzgün desenlidir,
# bu yüzden bölge içinde texOffs'un tam yeri önemli değildir; tek koşul, kutunun
# UV ayak izinin — yani 2*(w+d) x (h+d) — bölgeye sığmasıdır.
#
# Bir araç gövdesi 30 birim geniş ve 80 birim uzun olabildiğinden ayak izi
# 2*(30+80) = 220 birime çıkar; bu yüzden gövde ve cam bölgeleri 256 birim
# geniştir ve atlas 512x512'dir.
ATLAS_SIZE = 512
ATLAS_REGIONS = {
    "body":        (0, 0, 256, 128),
    "glass":       (256, 0, 256, 128),
    "trim":        (0, 128, 256, 128),
    "interior":    (256, 128, 256, 128),
    "tire":        (0, 256, 128, 128),
    "grille":      (128, 256, 128, 128),
    # Alt gövde neredeyse aracın tamamı kadar uzun, bu yüzden ayak izi 200
    # birimi aşabiliyor; bölge 256 birim geniş olmak zorunda.
    "under":       (256, 256, 256, 128),
    "rim_street":  (0, 384, 64, 64),
    "rim_sport":   (64, 384, 64, 64),
    "rim_offroad": (128, 384, 64, 64),
    "rim_chrome":  (192, 384, 64, 64),
    "light_front": (256, 384, 64, 64),
    "light_rear":  (320, 384, 64, 64),
    "chrome":      (384, 384, 128, 128),
    # Kapı ayrım çizgileri ve gölgeli oyuklar
    "body_dark":   (0, 448, 192, 64),
}


def gen_car_atlas():
    c = Canvas(ATLAS_SIZE, ATLAS_SIZE)

    # Gövde boyası: neredeyse beyaz, çalışma zamanında araç rengiyle
    # çarpılarak boyanır. Hafif dikey degrade metalik his verir.
    x, y, w, h = ATLAS_REGIONS["body"]
    c.rect(x, y, w, h, (236, 238, 242))
    c.vshade(x, y, w, h, 14, -30)
    c.noise(x, y, w, h, 3, seed=1)

    # Cam: koyu mavi-gri, üstte parlama. Çapraz yansıma şeritleri ikişerli
    # ve değişken parlaklıkta; böylece eğimli panellerde gerçek bir cam gibi
    # kırılan ışık izlenimi verir.
    x, y, w, h = ATLAS_REGIONS["glass"]
    c.rect(x, y, w, h, (30, 40, 54))
    c.vshade(x, y, w, h, 46, -14)
    # Şeritler bölgenin tamamını kaplasın diye köşegen desen sarmalı çizilir;
    # bölgeden taşan bir çizgi komşu malzemeyi bozardı.
    for py in range(h):
        for px in range(w):
            phase = (px + py) % 16
            if phase == 0:
                c.px_set(x + px, y + py, (86, 104, 128))
            elif phase == 3:
                c.px_set(x + px, y + py, (58, 72, 92))
    c.noise(x, y, w, h, 3, seed=9)

    # Siyah plastik trim / tampon
    x, y, w, h = ATLAS_REGIONS["trim"]
    c.rect(x, y, w, h, (48, 50, 56))
    c.noise(x, y, w, h, 5, seed=2)
    c.scanlines(x, y, w, h, 7, step=6)

    # Lastik: koyu kauçuk + diş deseni
    x, y, w, h = ATLAS_REGIONS["tire"]
    c.rect(x, y, w, h, (26, 26, 28))
    c.noise(x, y, w, h, 4, seed=3)
    for i in range(0, w, 8):
        c.vline(x + i, y, h, (14, 14, 16))
        c.vline(x + i + 1, y, h, (40, 40, 44))
        c.vline(x + i + 2, y, h, (34, 34, 38))

    # Jantlar
    _rim(c, ATLAS_REGIONS["rim_street"], spec.WHEELS["wheel_street"][4], spokes=8)
    _rim(c, ATLAS_REGIONS["rim_sport"], spec.WHEELS["wheel_sport"][4], spokes=5)
    _rim(c, ATLAS_REGIONS["rim_offroad"], spec.WHEELS["wheel_offroad"][4], spokes=6)
    _rim(c, ATLAS_REGIONS["rim_chrome"], spec.WHEELS["wheel_chrome"][4], spokes=10)

    # Farlar
    x, y, w, h = ATLAS_REGIONS["light_front"]
    c.rect(x, y, w, h, (226, 232, 238))
    c.vshade(x, y, w, h, 12, -46)
    for i in range(0, w, 4):
        c.vline(x + i, y, h, (250, 252, 255))
        c.vline(x + i + 2, y, h, (176, 190, 204))

    x, y, w, h = ATLAS_REGIONS["light_rear"]
    c.rect(x, y, w, h, (172, 26, 26))
    c.vshade(x, y, w, h, 26, -30)
    for i in range(0, w, 10):
        c.vline(x + i, y, h, (214, 48, 44))

    # Krom
    x, y, w, h = ATLAS_REGIONS["chrome"]
    c.rect(x, y, w, h, (196, 200, 208))
    c.vshade(x, y, w, h, 44, -56)
    c.noise(x, y, w, h, 4, seed=4)

    # İç mekan / koltuk
    x, y, w, h = ATLAS_REGIONS["interior"]
    c.rect(x, y, w, h, (48, 42, 40))
    c.noise(x, y, w, h, 6, seed=5)
    for i in range(0, h, 12):
        c.hline(x, y + i, w, (62, 55, 52))

    # Panjur (ön ızgara)
    x, y, w, h = ATLAS_REGIONS["grille"]
    c.rect(x, y, w, h, (34, 37, 42))
    for i in range(0, h, 3):
        c.hline(x, y + i, w, (86, 92, 102))
        c.hline(x, y + i + 1, w, (16, 17, 20))

    # Kapı ayrım çizgisi / koyu oyuk — boyayla çarpıldığında gövdenin koyu tonu
    x, y, w, h = ATLAS_REGIONS["body_dark"]
    c.rect(x, y, w, h, (78, 80, 84))

    # Alt gövde / şasi
    x, y, w, h = ATLAS_REGIONS["under"]
    c.rect(x, y, w, h, (40, 38, 36))
    c.noise(x, y, w, h, 7, seed=6)

    c.save(TEX / "entity/car/base.png")


def _rim(c, region, color, spokes):
    x, y, w, h = region
    c.rect(x, y, w, h, shade(color, -50))
    cx, cy = x + w // 2, y + h // 2
    r = min(w, h) // 2 - 1
    c.disc(cx, cy, r, color)
    c.ring(cx, cy, r, 2, shade(color, -40))
    c.disc(cx, cy, max(2, r // 3), shade(color, 30))
    import math
    for i in range(spokes):
        a = 2 * math.pi * i / spokes
        c.line(cx, cy, int(cx + r * 0.85 * math.cos(a)), int(cy + r * 0.85 * math.sin(a)),
               shade(color, -35))


# ==========================================================================
# Item ikonları (16x16)
# ==========================================================================

def gen_items():
    out = TEX / "item"

    # --- hammadde ---
    c = Canvas(16, 16)
    c.rect(3, 4, 10, 8, (168, 172, 180))
    c.vshade(3, 4, 10, 8, 26, -34)
    c.outline_rect(3, 4, 10, 8, (98, 102, 110))
    for i in (5, 10):
        c.px_set(i, 6, (222, 226, 232))
        c.px_set(i, 9, (120, 124, 132))
    c.save(out / "steel_plate.png")

    c = Canvas(16, 16)
    c.disc(8, 8, 6, (34, 34, 38))
    c.ring(8, 8, 6, 1, (18, 18, 20))
    c.disc(8, 8, 3, (52, 52, 58))
    c.noise(2, 2, 12, 12, 6, seed=11)
    c.save(out / "rubber.png")

    c = Canvas(16, 16)
    c.rect(3, 3, 10, 10, (150, 200, 220, 150))
    c.outline_rect(3, 3, 10, 10, (206, 236, 246))
    c.line(5, 11, 11, 5, (240, 252, 255))
    c.save(out / "glass_panel.png")

    c = Canvas(16, 16)  # koltuk
    c.rect(4, 2, 7, 8, (58, 50, 48))
    c.rect(3, 9, 9, 4, (48, 42, 40))
    c.outline_rect(4, 2, 7, 8, (32, 28, 27))
    c.outline_rect(3, 9, 9, 4, (32, 28, 27))
    c.hline(5, 4, 5, (76, 66, 62))
    c.hline(5, 6, 5, (76, 66, 62))
    c.save(out / "car_seat.png")

    c = Canvas(16, 16)  # ön cam
    for i in range(9):
        c.hline(3 + i // 3, 3 + i, 10 - 2 * (i // 4), (132, 174, 196, 170))
    c.line(3, 12, 6, 3, (222, 240, 248))
    c.line(12, 12, 9, 3, (222, 240, 248))
    c.hline(2, 12, 12, (60, 64, 70))
    c.save(out / "windshield.png")

    c = Canvas(16, 16)  # far
    c.rect(2, 5, 12, 7, (60, 62, 68))
    c.outline_rect(2, 5, 12, 7, (34, 36, 40))
    c.disc(7, 8, 4, (250, 248, 214))
    c.ring(7, 8, 4, 1, (206, 202, 160))
    c.disc(7, 8, 2, (255, 255, 250))
    c.save(out / "headlight.png")

    c = Canvas(16, 16)  # ingiliz anahtarı
    c.line(4, 12, 11, 4, (176, 180, 188))
    c.line(5, 12, 12, 4, (208, 212, 220))
    c.rect(9, 2, 5, 4, (196, 200, 208))
    c.rect(11, 2, 2, 2, (0, 0, 0, 0))
    c.rect(2, 10, 4, 4, (196, 200, 208))
    c.rect(2, 12, 2, 2, (0, 0, 0, 0))
    c.save(out / "wrench.png")

    c = Canvas(16, 16)  # yakıt bidonu
    c.rect(3, 4, 9, 10, (176, 48, 36))
    c.vshade(3, 4, 9, 10, 22, -34)
    c.outline_rect(3, 4, 9, 10, (94, 24, 18))
    c.rect(5, 2, 4, 2, (60, 62, 68))
    c.rect(12, 6, 2, 5, (140, 40, 30))
    c.hline(4, 8, 7, (210, 90, 74))
    c.save(out / "fuel_canister.png")

    c = Canvas(16, 16)  # katalog
    c.rect(3, 2, 10, 12, (60, 76, 128))
    c.outline_rect(3, 2, 10, 12, (30, 38, 66))
    c.rect(4, 3, 8, 10, (222, 216, 200))
    c.rect(4, 3, 1, 10, (150, 146, 134))
    for i in range(5, 12, 2):
        c.hline(6, i, 5, (110, 108, 100))
    c.save(out / "car_manual.png")

    # --- motorlar ---
    for eid, (cyl, _tr, _en, power) in spec.ENGINES.items():
        c = Canvas(16, 16)
        base = mix((92, 96, 104), (176, 60, 44), min(1.0, (power - 1.0) / 0.6))
        c.rect(3, 5, 10, 8, base)
        c.vshade(3, 5, 10, 8, 24, -32)
        c.outline_rect(3, 5, 10, 8, shade(base, -50))
        # silindir kapakları
        per_bank = cyl if "i" in eid.split("_")[1][:1] else cyl // 2
        n = min(4, max(2, per_bank))
        step = 10 // n
        for i in range(n):
            cx = 3 + step // 2 + i * step
            c.rect(cx, 2, max(1, step - 1), 3, (168, 172, 180))
            c.px_set(cx, 2, (216, 220, 228))
        c.rect(4, 12, 8, 2, (46, 44, 42))
        c.hline(4, 9, 8, shade(base, 34))
        c.save(out / f"{eid}.png")

    # --- şanzımanlar ---
    for tid, (_tr, _en, gears, auto, _r) in spec.TRANSMISSIONS.items():
        c = Canvas(16, 16)
        body = (120, 124, 132) if not auto else (108, 118, 140)
        c.rect(3, 6, 10, 7, body)
        c.vshade(3, 6, 10, 7, 22, -30)
        c.outline_rect(3, 6, 10, 7, shade(body, -48))
        c.rect(7, 2, 2, 4, (60, 62, 68))
        c.disc(8, 2, 2, (196, 40, 36) if not auto else (60, 140, 190))
        for i in range(min(gears, 5)):
            c.px_set(5 + i * 2, 10, (222, 226, 232))
        c.save(out / f"{tid}.png")

    # --- tekerlekler ---
    for wid, (_tr, _en, _g1, _g2, rim) in spec.WHEELS.items():
        c = Canvas(16, 16)
        c.disc(8, 8, 7, (28, 28, 30))
        c.ring(8, 8, 7, 1, (14, 14, 16))
        if wid == "wheel_offroad":
            for i in range(0, 16, 3):
                c.px_set(i, 8 - int((49 - (i - 8) ** 2) ** 0.5) if abs(i - 8) < 7 else 8, (48, 46, 42))
        c.disc(8, 8, 4, rim)
        c.ring(8, 8, 4, 1, shade(rim, -46))
        c.disc(8, 8, 1, shade(rim, 34))
        import math
        spokes = {"wheel_street": 8, "wheel_sport": 5, "wheel_offroad": 6, "wheel_chrome": 10}[wid]
        for i in range(spokes):
            a = 2 * math.pi * i / spokes
            c.line(8, 8, int(8 + 3.4 * math.cos(a)), int(8 + 3.4 * math.sin(a)), shade(rim, -38))
        c.save(out / f"{wid}.png")

    # --- rüzgarlıklar ---
    spoiler_shapes = {
        "spoiler_lip": [(2, 10, 12, 2), (3, 9, 10, 1)],
        "spoiler_ducktail": [(2, 11, 12, 2), (3, 9, 10, 2), (4, 8, 8, 1)],
        "spoiler_gt": [(2, 5, 12, 2), (4, 7, 2, 6), (10, 7, 2, 6), (2, 12, 12, 2)],
    }
    for sid, shapes in spoiler_shapes.items():
        c = Canvas(16, 16)
        for (x, y, w, h) in shapes:
            c.rect(x, y, w, h, (44, 46, 52))
            c.hline(x, y, w, (92, 96, 104))
        c.save(out / f"{sid}.png")

    # --- sprey boyalar ---
    for name, rgb in spec.DYE_COLORS.items():
        c = Canvas(16, 16)
        c.rect(5, 4, 6, 10, (196, 200, 208))
        c.vshade(5, 4, 6, 10, 26, -36)
        c.outline_rect(5, 4, 6, 10, (98, 102, 110))
        c.rect(5, 7, 6, 4, rgb)
        c.rect(6, 2, 4, 2, (70, 72, 78))
        c.px_set(10, 1, rgb)
        c.px_set(12, 0, rgb)
        c.px_set(11, 3, rgb)
        c.save(out / f"{spec.spray_can_id(name)}.png")

    # --- şasiler ---
    chassis_len = {"chassis_compact": 10, "chassis_sport": 12, "chassis_offroad": 11,
                   "chassis_classic": 10, "chassis_van": 12}
    for cid, ln in chassis_len.items():
        c = Canvas(16, 16)
        x0 = (16 - ln) // 2
        c.rect(x0, 6, ln, 2, (108, 112, 120))
        c.rect(x0, 10, ln, 2, (108, 112, 120))
        c.rect(x0, 6, 2, 6, (128, 132, 140))
        c.rect(x0 + ln - 2, 6, 2, 6, (128, 132, 140))
        c.hline(x0, 8, ln, (78, 82, 90))
        if cid == "chassis_offroad":
            c.rect(x0 + 1, 4, ln - 2, 2, (92, 96, 104))
        if cid == "chassis_sport":
            c.hline(x0, 9, ln, (176, 60, 44))
        if cid == "chassis_van":
            c.rect(x0 + 2, 4, ln - 4, 2, (92, 96, 104))
            c.vline(x0 + ln // 2, 4, 8, (78, 82, 90))
        c.save(out / f"{cid}.png")

    # --- araba item'ları: yandan siluet ---
    for car_id, car in spec.CARS.items():
        _car_icon(car).save(out / f"{spec.car_item_id(car_id)}.png")


def _car_icon(car):
    """Sınıfa göre 16x16 yandan araç silueti."""
    c = Canvas(16, 16)
    col = car["color"]
    dark = shade(col, -55)
    glass = (86, 118, 142)
    k = car["klass"]

    if k in ("sport",):
        body = [(1, 9, 14, 3)]
        roof = [(4, 7, 7, 2), (5, 6, 5, 1)]
    elif k in ("hatchback", "sedan"):
        body = [(1, 9, 14, 3)]
        roof = [(4, 6, 7, 3)]
    elif k in ("offroad", "suv"):
        body = [(1, 8, 14, 4)]
        roof = [(3, 4, 9, 4)]
    elif k == "pickup":
        body = [(1, 8, 14, 4)]
        roof = [(3, 5, 6, 3)]
    elif k == "van":
        body = [(1, 6, 14, 6)]
        roof = [(2, 3, 12, 3)]
    else:  # classic
        body = [(2, 8, 12, 4)]
        roof = [(5, 5, 6, 3)]

    for (x, y, w, h) in body:
        c.rect(x, y, w, h, col)
        c.vshade(x, y, w, h, 18, -28)
    for (x, y, w, h) in roof:
        c.rect(x, y, w, h, col)
        c.vshade(x, y, w, h, 22, -18)
        c.rect(x + 1, y + 1, w - 2, max(1, h - 2), glass)

    # tekerlekler
    for wx in (3, 12):
        c.disc(wx, 12, 2, (24, 24, 26))
        c.px_set(wx, 12, (150, 152, 158))
    # far & stop
    c.px_set(15, 10, (250, 246, 200))
    c.px_set(0, 10, (200, 40, 36))
    # alt gölge
    c.hline(2, 14, 12, (0, 0, 0, 60))
    for (x, y, w, h) in body:
        c.hline(x, y + h - 1, w, dark)
    return c


# ==========================================================================
# Blok dokuları
# ==========================================================================

def gen_blocks():
    out = TEX / "block"

    # asfalt
    c = Canvas(16, 16)
    c.rect(0, 0, 16, 16, (48, 48, 52))
    c.noise(0, 0, 16, 16, 12, seed=21)
    for (x, y) in ((2, 3), (7, 1), (11, 6), (4, 9), (13, 12), (8, 13), (1, 11)):
        c.px_set(x, y, (72, 72, 78))
    c.save(out / "asphalt.png")

    for name, line in (("road_line_white", (226, 228, 232)), ("road_line_yellow", (226, 186, 46))):
        c = Canvas(16, 16)
        c.rect(0, 0, 16, 16, (48, 48, 52))
        c.noise(0, 0, 16, 16, 12, seed=22)
        c.rect(6, 0, 4, 16, line)
        c.noise(6, 0, 4, 16, 8, seed=23)
        c.save(out / f"{name}.png")

    # montaj tezgahı
    c = Canvas(16, 16)  # yan
    c.rect(0, 0, 16, 16, (86, 66, 46))
    c.noise(0, 0, 16, 16, 8, seed=24)
    c.rect(0, 0, 16, 3, (108, 112, 120))
    c.hline(0, 3, 16, (52, 40, 28))
    for i in range(1, 16, 4):
        c.vline(i, 4, 12, (66, 50, 34))
    c.save(out / "assembly_table_side.png")

    c = Canvas(16, 16)  # üst
    c.rect(0, 0, 16, 16, (120, 124, 132))
    c.noise(0, 0, 16, 16, 10, seed=25)
    c.outline_rect(0, 0, 16, 16, (78, 82, 90))
    c.rect(2, 2, 5, 5, (60, 62, 68))
    c.rect(9, 3, 5, 2, (168, 172, 180))
    c.rect(9, 9, 5, 5, (60, 62, 68))
    c.line(2, 9, 6, 13, (196, 200, 208))
    c.save(out / "assembly_table_top.png")

    c = Canvas(16, 16)  # alt (tüm bloklar için ortak ahşap)
    c.rect(0, 0, 16, 16, (72, 55, 38))
    c.noise(0, 0, 16, 16, 7, seed=26)
    c.save(out / "assembly_table_bottom.png")

    # araba yapma masası: koyu çelik tezgah, üstünde araç şablonu
    c = Canvas(16, 16)  # yan
    c.rect(0, 0, 16, 16, (74, 78, 86))
    c.noise(0, 0, 16, 16, 6, seed=41)
    c.rect(0, 3, 16, 2, (44, 46, 52))
    c.rect(0, 11, 16, 2, (44, 46, 52))
    for i in range(2, 15, 4):
        c.rect(i, 6, 2, 4, (156, 160, 168))
    c.save(out / "car_workbench_side.png")

    c = Canvas(16, 16)  # üst — tepeden araç şeması
    c.rect(0, 0, 16, 16, (96, 100, 108))
    c.noise(0, 0, 16, 16, 8, seed=42)
    c.outline_rect(0, 0, 16, 16, (58, 60, 66))
    c.rect(5, 2, 6, 12, (188, 192, 200))
    c.rect(6, 4, 4, 3, (52, 78, 110))
    c.rect(6, 9, 4, 3, (52, 78, 110))
    for y in (3, 11):
        c.rect(3, y, 2, 2, (30, 30, 32))
        c.rect(11, y, 2, 2, (30, 30, 32))
    c.save(out / "car_workbench_top.png")

    # araç lifti
    c = Canvas(16, 16)  # yan
    c.rect(0, 0, 16, 16, (58, 60, 66))
    c.noise(0, 0, 16, 16, 6, seed=27)
    c.rect(1, 1, 14, 3, (226, 168, 32))
    for i in range(1, 15, 3):
        c.line(i, 1, i + 2, 3, (46, 46, 50))
    c.rect(6, 5, 4, 11, (96, 100, 108))
    c.save(out / "car_lift_side.png")

    c = Canvas(16, 16)  # üst
    c.rect(0, 0, 16, 16, (72, 74, 80))
    c.noise(0, 0, 16, 16, 6, seed=28)
    c.rect(1, 3, 14, 4, (226, 168, 32))
    c.rect(1, 9, 14, 4, (226, 168, 32))
    for i in range(0, 16, 4):
        c.vline(i, 3, 4, (46, 46, 50))
        c.vline(i, 9, 4, (46, 46, 50))
    c.save(out / "car_lift_top.png")

    # yakıt pompası — ön yüzde gösterge paneli, yanlarda düz gövde
    c = Canvas(16, 16)
    c.rect(1, 0, 14, 16, (188, 52, 40))
    c.vshade(1, 0, 14, 16, 18, -30)
    c.outline_rect(1, 0, 14, 16, (98, 26, 20))
    c.rect(3, 2, 10, 5, (28, 32, 40))
    c.rect(4, 3, 8, 3, (110, 220, 140))
    c.hline(4, 4, 8, (40, 60, 46))
    c.rect(3, 9, 10, 2, (150, 40, 30))
    c.rect(11, 11, 3, 4, (60, 62, 68))
    c.save(out / "fuel_pump_front.png")

    c = Canvas(16, 16)
    c.rect(1, 0, 14, 16, (176, 48, 36))
    c.vshade(1, 0, 14, 16, 18, -30)
    c.outline_rect(1, 0, 14, 16, (98, 26, 20))
    c.noise(2, 1, 12, 14, 5, seed=30)
    c.hline(2, 8, 12, (140, 38, 28))
    c.save(out / "fuel_pump_side.png")

    c = Canvas(16, 16)
    c.rect(0, 0, 16, 16, (150, 42, 32))
    c.noise(0, 0, 16, 16, 6, seed=29)
    c.rect(4, 4, 8, 8, (60, 62, 68))
    c.save(out / "fuel_pump_top.png")


# ==========================================================================
# HUD ve GUI
# ==========================================================================

def gen_gui():
    out = TEX / "gui"

    # --- hız göstergesi kadranı (128x128, sol üstte 128x72 yarım kadran) ---
    import math
    c = Canvas(128, 128)
    cx, cy, r = 64, 64, 58
    # dış halka
    c.disc(cx, cy, r, (18, 20, 24, 210))
    c.ring(cx, cy, r, 3, (96, 102, 112, 255))
    c.ring(cx, cy, r - 4, 1, (54, 58, 66, 255))
    # 220 derecelik yay üzerinde çentikler (sol-alt -> sağ-alt)
    start, sweep = 160.0, 220.0
    for i in range(23):
        t = i / 22.0
        a = math.radians(start + sweep * t)
        major = (i % 2 == 0)
        ln = 9 if major else 5
        col = (232, 236, 244) if t < 0.8 else (226, 72, 58)
        x0 = cx + (r - 7) * math.cos(a)
        y0 = cy + (r - 7) * math.sin(a)
        x1 = cx + (r - 7 - ln) * math.cos(a)
        y1 = cy + (r - 7 - ln) * math.sin(a)
        c.line(int(x0), int(y0), int(x1), int(y1), col)
        if major:
            c.line(int(x0 + 0.5), int(y0), int(x1 + 0.5), int(y1), col)
    # kırmızı bölge yayı
    for i in range(60):
        t = 0.82 + 0.18 * i / 59.0
        a = math.radians(start + sweep * t)
        c.line(int(cx + (r - 5) * math.cos(a)), int(cy + (r - 5) * math.sin(a)),
               int(cx + (r - 8) * math.cos(a)), int(cy + (r - 8) * math.sin(a)), (206, 46, 38))
    c.disc(cx, cy, 6, (140, 146, 156))
    c.ring(cx, cy, 6, 1, (40, 42, 48))
    c.save(out / "speedometer_dial.png")

    # --- ibre (16x64, aşağıdan yukarı, döndürülerek çizilir) ---
    c = Canvas(16, 64)
    for y in range(6, 60):
        t = (y - 6) / 54.0
        half = max(0, int(3 * (1 - t)))
        c.rect(8 - half - 1, 63 - y, 2 * half + 2, 1, (226, 62, 48))
    c.rect(6, 56, 4, 6, (150, 30, 24))
    c.save(out / "speedometer_needle.png")

    # --- HUD parçaları: yakıt/devir çubuğu çerçeveleri (64x32) ---
    c = Canvas(64, 32)
    c.rect(0, 0, 64, 12, (18, 20, 24, 200))
    c.outline_rect(0, 0, 64, 12, (96, 102, 112))
    c.rect(0, 16, 64, 12, (18, 20, 24, 200))
    c.outline_rect(0, 16, 64, 12, (96, 102, 112))
    c.save(out / "hud_bars.png")

    # --- modifiye ekranı arka planı ---
    # Panel 248x225: üstte sekme şeridi, solda 3B önizleme çukuru, sağda
    # seçenek alanı, altta oyuncu envanteri. Ölçüler CarModificationScreen
    # içindeki sabitlerle birebir aynıdır.
    c = Canvas(256, 256)
    _panel(c, 0, 0, 248, 235)
    _inset(c, 7, 36, 94, 100)      # önizleme
    _inset(c, 106, 36, 135, 100)   # seçenekler
    for row in range(3):
        for col in range(9):
            _slot(c, 7 + col * 18, 150 + row * 18)
    for col in range(9):
        _slot(c, 7 + col * 18, 210)
    c.save(out / "modification.png")

    # --- montaj tezgahı ekranı arka planı ---
    c = Canvas(256, 256)
    _panel(c, 0, 0, 176, 166)
    for row in range(3):
        for col in range(3):
            _slot(c, 29 + col * 18, 16 + row * 18)
    _slot(c, 123, 34)
    _arrow(c, 90, 34)
    for row in range(3):
        for col in range(9):
            _slot(c, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        _slot(c, 7 + col * 18, 141)
    c.save(out / "assembly_table.png")

    # --- araba yapma masası ekranı ---
    # Altı girdi yuvası tek sıra: şasi, motor, şanzıman, tekerlek, koltuk, ön cam
    c = Canvas(256, 256)
    _panel(c, 0, 0, 176, 166)
    for col in range(6):
        _slot(c, 7 + col * 18, 34)
    _arrow(c, 120, 34)
    _slot(c, 144, 34)
    for row in range(3):
        for col in range(9):
            _slot(c, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        _slot(c, 7 + col * 18, 141)
    c.save(out / "car_workbench.png")

    # sekme ikonları (renk / tekerlek / rüzgarlık / motor)
    icons = Canvas(64, 16)
    _tab_icon_color(icons, 0)
    _tab_icon_wheel(icons, 16)
    _tab_icon_spoiler(icons, 32)
    _tab_icon_engine(icons, 48)
    icons.save(out / "mod_tabs.png")


def _panel(c, x, y, w, h):
    c.rect(x, y, w, h, (198, 198, 198))
    c.hline(x, y, w, (255, 255, 255))
    c.vline(x, y, h, (255, 255, 255))
    c.hline(x, y + h - 1, w, (85, 85, 85))
    c.vline(x + w - 1, y, h, (85, 85, 85))


def _inset(c, x, y, w, h):
    c.rect(x, y, w, h, (139, 139, 139))
    c.hline(x, y, w, (85, 85, 85))
    c.vline(x, y, h, (85, 85, 85))
    c.hline(x, y + h - 1, w, (255, 255, 255))
    c.vline(x + w - 1, y, h, (255, 255, 255))


def _slot(c, x, y):
    _inset(c, x, y, 18, 18)


def _arrow(c, x, y):
    c.rect(x, y + 6, 14, 4, (85, 85, 85))
    for i in range(5):
        c.vline(x + 14 + i, y + 2 + i, 12 - 2 * i, (85, 85, 85))


def _tab_icon_color(c, x):
    for i, col in enumerate([(200, 40, 36), (40, 90, 190), (60, 160, 60), (220, 190, 40)]):
        c.rect(x + 2 + (i % 2) * 6, 3 + (i // 2) * 6, 5, 5, col)


def _tab_icon_wheel(c, x):
    c.disc(x + 8, 8, 6, (30, 30, 32))
    c.disc(x + 8, 8, 3, (170, 174, 182))


def _tab_icon_spoiler(c, x):
    c.rect(x + 2, 5, 12, 2, (60, 62, 68))
    c.rect(x + 4, 7, 2, 5, (60, 62, 68))
    c.rect(x + 10, 7, 2, 5, (60, 62, 68))


def _tab_icon_engine(c, x):
    c.rect(x + 3, 6, 10, 7, (140, 60, 48))
    c.rect(x + 5, 3, 2, 3, (170, 174, 182))
    c.rect(x + 9, 3, 2, 3, (170, 174, 182))


def gen_icon():
    """Mod paketi ikonu."""
    c = Canvas(128, 128)
    c.rect(0, 0, 128, 128, (38, 42, 52))
    c.rect(0, 92, 128, 36, (48, 48, 52))
    for x in range(6, 128, 26):
        c.rect(x, 108, 14, 4, (226, 228, 232))
    car = _car_icon(spec.CARS["mustang_gt"])
    big = Canvas(128, 128)
    for y in range(16):
        for x in range(16):
            px = car.px[y, x]
            if px[3] > 0:
                big.rect(x * 7 + 8, y * 5 + 12, 7, 5, tuple(int(v) for v in px))
    c.blit(big, 0, 0)
    c.save(ROOT / "src/main/resources/assets" / spec.MOD_ID / "icon.png")


def main():
    gen_car_atlas()
    gen_items()
    gen_blocks()
    gen_gui()
    gen_icon()
    n = len(list(TEX.rglob("*.png")))
    print(f"{n} doku üretildi -> {TEX}")


if __name__ == "__main__":
    main()
