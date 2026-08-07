"""RealCars içerik tanımı — tüm üreticilerin ortak kaynağı.

Buradaki tablolar hem doku/ses/JSON üreticileri hem de doğrulayıcı tarafından
kullanılır. Java tarafındaki `CarModel`, `EngineType`, `TransmissionType` ve
`WheelType` enum'ları bu tabloyla birebir eşleşmek zorundadır;
`validate_assets.py` bu eşleşmeyi kontrol eder.
"""

MOD_ID = "realcars"

# --------------------------------------------------------------------------
# Motor / şanzıman / tekerlek / rüzgarlık türleri
# --------------------------------------------------------------------------

# id -> (silindir, tr_ad, en_ad, güç çarpanı)
ENGINES = {
    "engine_i4":    (4, "4 Silindir Sıra Motor", "Inline-4 Engine", 1.00),
    "engine_i6":    (6, "6 Silindir Sıra Motor", "Inline-6 Engine", 1.30),
    "engine_v6":    (6, "V6 Motor", "V6 Engine", 1.25),
    "engine_v8":    (8, "V8 Motor", "V8 Engine", 1.55),
    "engine_flat6": (6, "Boksör 6 Motor", "Flat-6 Engine", 1.40),
}

# id -> (tr_ad, en_ad, vites sayısı, otomatik mi, vites geçiş hızı çarpanı)
TRANSMISSIONS = {
    "transmission_manual":    ("Manuel Şanzıman", "Manual Transmission", 6, False, 1.00),
    "transmission_automatic": ("Otomatik Şanzıman", "Automatic Transmission", 6, True, 0.90),
    "transmission_sport":     ("Spor Çift Kavramalı Şanzıman", "Sport Dual-Clutch", 7, True, 1.15),
}

# id -> (tr_ad, en_ad, asfalt tutuşu, arazi tutuşu, jant rengi)
WHEELS = {
    "wheel_street":  ("Sokak Tekerleği", "Street Wheel", 1.00, 0.55, (150, 152, 158)),
    "wheel_sport":   ("Spor Tekerlek", "Sport Wheel", 1.18, 0.42, (60, 62, 68)),
    "wheel_offroad": ("Arazi Tekerleği", "Off-road Wheel", 0.82, 1.00, (70, 66, 58)),
    "wheel_chrome":  ("Krom Jant", "Chrome Wheel", 1.05, 0.50, (225, 228, 236)),
}

# id -> (tr_ad, en_ad, bastırma kuvveti = viraj tutuş bonusu)
SPOILERS = {
    "spoiler_none":     ("Rüzgarlıksız", "No Spoiler", 0.00),
    "spoiler_lip":      ("Lip Rüzgarlık", "Lip Spoiler", 0.04),
    "spoiler_ducktail": ("Ördek Kuyruğu Rüzgarlık", "Ducktail Spoiler", 0.07),
    "spoiler_gt":       ("GT Kanat", "GT Wing", 0.12),
}

# --------------------------------------------------------------------------
# Araç listesi
# --------------------------------------------------------------------------
# id -> dict(...)
#   tr / en      : görünen isim
#   klass        : hatchback | sedan | sport | offroad | pickup | suv | classic | van
#   chassis      : şasi parçası (craft girdisi)
#   engine       : varsayılan motor
#   trans        : varsayılan şanzıman
#   wheel        : varsayılan tekerlek
#   top_kmh      : maksimum hız (km/s)
#   accel        : 0->100 km/s süresi (saniye), fizik ivmesine çevrilir
#   mass         : kütle (kg) — tırmanma ve çarpışma için
#   fuel         : yakıt kapasitesi (litre)
#   color        : fabrika çıkış rengi (RGB)

CARS = {
    # --- Hatchback / Sedan --------------------------------------------------
    "golf_gti": dict(
        tr="Volkswagen Golf GTI", en="Volkswagen Golf GTI", klass="hatchback",
        chassis="chassis_compact", engine="engine_i4", trans="transmission_manual",
        wheel="wheel_sport", top_kmh=250, accel=6.2, mass=1400, fuel=50,
        color=(0xC7, 0x1F, 0x24)),
    "corolla": dict(
        tr="Toyota Corolla", en="Toyota Corolla", klass="sedan",
        chassis="chassis_compact", engine="engine_i4", trans="transmission_automatic",
        wheel="wheel_street", top_kmh=190, accel=10.4, mass=1320, fuel=50,
        color=(0xD8, 0xDC, 0xE2)),
    "bmw_m3": dict(
        tr="BMW M3", en="BMW M3", klass="sedan",
        chassis="chassis_sport", engine="engine_i6", trans="transmission_manual",
        wheel="wheel_sport", top_kmh=290, accel=4.1, mass=1730, fuel=59,
        color=(0x1B, 0x54, 0xA8)),

    # --- Spor ---------------------------------------------------------------
    "mustang_gt": dict(
        tr="Ford Mustang GT", en="Ford Mustang GT", klass="sport",
        chassis="chassis_sport", engine="engine_v8", trans="transmission_manual",
        wheel="wheel_sport", top_kmh=250, accel=4.3, mass=1740, fuel=61,
        color=(0x16, 0x1A, 0x1F)),
    "gtr": dict(
        tr="Nissan GT-R", en="Nissan GT-R", klass="sport",
        chassis="chassis_sport", engine="engine_v6", trans="transmission_sport",
        wheel="wheel_sport", top_kmh=315, accel=2.9, mass=1750, fuel=74,
        color=(0x6E, 0x76, 0x82)),
    "porsche_911": dict(
        tr="Porsche 911", en="Porsche 911", klass="sport",
        chassis="chassis_sport", engine="engine_flat6", trans="transmission_sport",
        wheel="wheel_sport", top_kmh=300, accel=3.4, mass=1520, fuel=64,
        color=(0xE4, 0xE7, 0xEC)),

    # --- Off-road / SUV / Pickup -------------------------------------------
    "wrangler": dict(
        tr="Jeep Wrangler", en="Jeep Wrangler", klass="offroad",
        chassis="chassis_offroad", engine="engine_v6", trans="transmission_automatic",
        wheel="wheel_offroad", top_kmh=180, accel=7.6, mass=1900, fuel=70,
        color=(0x3E, 0x6B, 0x35)),
    "hilux": dict(
        tr="Toyota Hilux", en="Toyota Hilux", klass="pickup",
        chassis="chassis_offroad", engine="engine_i4", trans="transmission_manual",
        wheel="wheel_offroad", top_kmh=175, accel=11.0, mass=2050, fuel=80,
        color=(0xE8, 0xEB, 0xEF)),
    "defender": dict(
        tr="Land Rover Defender", en="Land Rover Defender", klass="suv",
        chassis="chassis_offroad", engine="engine_i6", trans="transmission_automatic",
        wheel="wheel_offroad", top_kmh=190, accel=6.6, mass=2320, fuel=90,
        color=(0x8B, 0x8F, 0x7A)),

    # --- Klasik / Kamyonet --------------------------------------------------
    "beetle": dict(
        tr="Volkswagen Beetle", en="Volkswagen Beetle", klass="classic",
        chassis="chassis_classic", engine="engine_i4", trans="transmission_manual",
        wheel="wheel_street", top_kmh=130, accel=17.5, mass=820, fuel=40,
        color=(0x62, 0xA8, 0xD8)),
    "c10": dict(
        tr="Chevrolet C10", en="Chevrolet C10", klass="pickup",
        chassis="chassis_classic", engine="engine_v8", trans="transmission_manual",
        wheel="wheel_street", top_kmh=160, accel=9.0, mass=1680, fuel=76,
        color=(0x2E, 0x6B, 0x52)),
    "transporter": dict(
        tr="Volkswagen Transporter T1", en="Volkswagen Transporter T1", klass="van",
        chassis="chassis_van", engine="engine_i4", trans="transmission_manual",
        wheel="wheel_street", top_kmh=105, accel=22.0, mass=1180, fuel=42,
        color=(0xC4, 0x5A, 0x3A)),
}

# --------------------------------------------------------------------------
# Boya renkleri (vanilla boya adlarıyla eşleşir)
# --------------------------------------------------------------------------
DYE_COLORS = {
    "white":      (0xF0, 0xF2, 0xF5),
    "orange":     (0xE3, 0x74, 0x1E),
    "magenta":    (0xC0, 0x4A, 0xC4),
    "light_blue": (0x54, 0xA5, 0xDA),
    "yellow":     (0xE8, 0xC6, 0x2E),
    "lime":       (0x76, 0xC7, 0x2B),
    "pink":       (0xE9, 0x8D, 0xB0),
    "gray":       (0x50, 0x54, 0x59),
    "light_gray": (0x9C, 0xA1, 0xA7),
    "cyan":       (0x22, 0x9B, 0xA8),
    "purple":     (0x7B, 0x35, 0xB5),
    "blue":       (0x2C, 0x44, 0xB0),
    "brown":      (0x7A, 0x51, 0x30),
    "green":      (0x46, 0x77, 0x22),
    "red":        (0xB5, 0x27, 0x24),
    "black":      (0x1A, 0x1C, 0x1F),
}

# --------------------------------------------------------------------------
# Diğer item'lar
# --------------------------------------------------------------------------
# id -> (tr_ad, en_ad)
MATERIALS = {
    "steel_plate":  ("Çelik Levha", "Steel Plate"),
    "rubber":       ("Kauçuk", "Rubber"),
    "glass_panel":  ("Cam Panel", "Glass Panel"),
    "car_seat":     ("Araç Koltuğu", "Car Seat"),
    "windshield":   ("Ön Cam", "Windshield"),
    "headlight":    ("Far", "Headlight"),
    "wrench":       ("İngiliz Anahtarı", "Wrench"),
    "fuel_canister": ("Yakıt Bidonu", "Fuel Canister"),
    "car_manual":   ("Araç Kataloğu", "Car Catalog"),
}

CHASSIS = {
    "chassis_compact": ("Kompakt Şasi", "Compact Chassis"),
    "chassis_sport":   ("Spor Şasi", "Sport Chassis"),
    "chassis_offroad": ("Arazi Şasisi", "Off-road Chassis"),
    "chassis_classic": ("Klasik Şasi", "Classic Chassis"),
    "chassis_van":     ("Minibüs Şasisi", "Van Chassis"),
}

BLOCKS = {
    "assembly_table":   ("Montaj Tezgahı", "Assembly Table"),
    "car_lift":         ("Araç Lifti", "Car Lift"),
    "fuel_pump":        ("Yakıt Pompası", "Fuel Pump"),
    "asphalt":          ("Asfalt", "Asphalt"),
    "asphalt_slab":     ("Asfalt Levha", "Asphalt Slab"),
    "road_line_white":  ("Beyaz Yol Çizgisi", "White Road Line"),
    "road_line_yellow": ("Sarı Yol Çizgisi", "Yellow Road Line"),
}

# --------------------------------------------------------------------------
# Sesler
# --------------------------------------------------------------------------
# id -> (tr açıklama, süre sn, döngü mü)
SOUNDS = {
    "engine_idle":       ("Rölanti", 2.0, True),
    "engine_loop_i4":    ("4 silindir motor", 2.0, True),
    "engine_loop_i6":    ("6 silindir sıra motor", 2.0, True),
    "engine_loop_v6":    ("V6 motor", 2.0, True),
    "engine_loop_v8":    ("V8 motor", 2.0, True),
    "engine_loop_flat6": ("Boksör 6 motor", 2.0, True),
    "engine_start":      ("Marş", 1.4, False),
    "engine_stop":       ("Motor durdurma", 0.9, False),
    "horn":              ("Korna", 0.7, False),
    "brake_squeal":      ("Fren gıcırtısı", 0.8, False),
    "gravel_loop":       ("Çakıl/toprak sürüş", 1.5, True),
    "crash":             ("Çarpışma", 0.9, False),
    "door_close":        ("Kapı kapanma", 0.5, False),
    "wrench_use":        ("Anahtar sesi", 0.6, False),
}

# Motor id -> ses id eşlemesi
ENGINE_SOUND = {
    "engine_i4": "engine_loop_i4",
    "engine_i6": "engine_loop_i6",
    "engine_v6": "engine_loop_v6",
    "engine_v8": "engine_loop_v8",
    "engine_flat6": "engine_loop_flat6",
}


def car_item_id(car_id: str) -> str:
    return f"car_{car_id}"


def spray_can_id(color: str) -> str:
    return f"spray_can_{color}"


def all_item_ids():
    """Modun kaydettiği tüm item id'leri."""
    ids = list(MATERIALS) + list(CHASSIS) + list(ENGINES) + list(TRANSMISSIONS)
    ids += list(WHEELS)
    ids += [s for s in SPOILERS if s != "spoiler_none"]
    ids += [spray_can_id(c) for c in DYE_COLORS]
    ids += [car_item_id(c) for c in CARS]
    ids += list(BLOCKS)
    return ids
