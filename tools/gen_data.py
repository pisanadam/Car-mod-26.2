#!/usr/bin/env python3
"""RealCars veri/kaynak JSON üreticisi.

Tarifler, item ve blok modelleri, blockstate'ler, ganimet tabloları, blok
etiketleri, sounds.json ve dil dosyalarını üretir.
"""

import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import spec  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets" / spec.MOD_ID
DATA = RES / "data" / spec.MOD_ID
NS = spec.MOD_ID


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def mid(name):
    """Bu modun ad alanındaki id."""
    return f"{NS}:{name}"


# ==========================================================================
# Tarifler
# ==========================================================================

def shaped(pattern, key, result, count=1, category="misc"):
    """Şekilli tarif.

    Kalıplarda boş slot okunaklı olsun diye '_' yazılır; Minecraft boş slotu
    boşluk karakteriyle beklediği için burada çevrilir.
    """
    return {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "key": key,
        "pattern": [row.replace("_", " ") for row in pattern],
        "result": {"id": result, **({"count": count} if count != 1 else {})},
    }


def shapeless(ingredients, result, count=1, category="misc"):
    return {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": ingredients,
        "result": {"id": result, **({"count": count} if count != 1 else {})},
    }


S = mid("steel_plate")
R = mid("rubber")
G = mid("glass_panel")

# Motor tarifleri — her biri benzersiz bir kalıp/anahtar bileşimi kullanır.
ENGINE_RECIPES = {
    "engine_i4":    (["_P_", "SIS", "SRS"], {"P": "minecraft:piston", "S": S,
                                             "I": "minecraft:iron_block", "R": "minecraft:redstone_block"}),
    "engine_i6":    (["PPP", "SIS", "SRS"], {"P": "minecraft:piston", "S": S,
                                             "I": "minecraft:iron_block", "R": "minecraft:redstone_block"}),
    "engine_v6":    (["P_P", "SIS", "SRS"], {"P": "minecraft:piston", "S": S,
                                             "I": "minecraft:iron_block", "R": "minecraft:redstone_block"}),
    "engine_v8":    (["PPP", "SDS", "SRS"], {"P": "minecraft:piston", "S": S,
                                             "D": "minecraft:diamond", "R": "minecraft:redstone_block"}),
    "engine_flat6": (["P_P", "SIS", "SCS"], {"P": "minecraft:piston", "S": S,
                                             "I": "minecraft:iron_block", "C": "minecraft:copper_block"}),
}

TRANSMISSION_RECIPES = {
    "transmission_manual":    (["_N_", "SIS", "_R_"], {"N": "minecraft:iron_nugget", "S": S,
                                                       "I": "minecraft:iron_block", "R": "minecraft:redstone"}),
    "transmission_automatic": (["_N_", "SIS", "_C_"], {"N": "minecraft:iron_nugget", "S": S,
                                                       "I": "minecraft:iron_block", "C": "minecraft:comparator"}),
    "transmission_sport":     (["_N_", "SDS", "_C_"], {"N": "minecraft:iron_nugget", "S": S,
                                                       "D": "minecraft:diamond", "C": "minecraft:comparator"}),
}

WHEEL_RECIPES = {
    "wheel_street":  (["_R_", "RIR", "_R_"], {"R": R, "I": "minecraft:iron_ingot"}),
    "wheel_sport":   (["_R_", "RSR", "_R_"], {"R": R, "S": S}),
    "wheel_offroad": (["RRR", "RIR", "RRR"], {"R": R, "I": "minecraft:iron_ingot"}),
    "wheel_chrome":  (["_R_", "RQR", "_R_"], {"R": R, "Q": "minecraft:quartz"}),
}

CHASSIS_RECIPES = {
    "chassis_compact": (["SSS", "S_S", "SSS"], {"S": S}),
    "chassis_sport":   (["SSS", "SDS", "SSS"], {"S": S, "D": "minecraft:diamond"}),
    "chassis_offroad": (["SSS", "SIS", "SSS"], {"S": S, "I": "minecraft:iron_block"}),
    "chassis_classic": (["SSS", "SCS", "SSS"], {"S": S, "C": "minecraft:copper_block"}),
    "chassis_van":     (["SSS", "SGS", "SSS"], {"S": S, "G": "minecraft:gold_block"}),
}

SPOILER_RECIPES = {
    "spoiler_lip":      (["SSS"], {"S": S}),
    "spoiler_ducktail": (["_S_", "SSS"], {"S": S}),
    "spoiler_gt":       (["SSS", "S_S"], {"S": S}),
}


def gen_recipes():
    out = DATA / "recipe"

    # --- hammadde ---
    write(out / "steel_plate.json",
          shaped(["II", "II"], {"I": "minecraft:iron_ingot"}, mid("steel_plate"), 4, "building"))
    write(out / "rubber.json",
          shapeless(["minecraft:slime_ball", "minecraft:slime_ball", "minecraft:coal"],
                    mid("rubber"), 3))
    write(out / "glass_panel.json",
          shaped(["GG", "GG"], {"G": "minecraft:glass"}, mid("glass_panel"), 4, "building"))

    # --- bileşenler ---
    for eid, (pattern, key) in ENGINE_RECIPES.items():
        write(out / f"{eid}.json", shaped(pattern, key, mid(eid), 1, "equipment"))
    for tid, (pattern, key) in TRANSMISSION_RECIPES.items():
        write(out / f"{tid}.json", shaped(pattern, key, mid(tid), 1, "equipment"))
    for wid, (pattern, key) in WHEEL_RECIPES.items():
        write(out / f"{wid}.json", shaped(pattern, key, mid(wid), 2, "equipment"))
    for cid, (pattern, key) in CHASSIS_RECIPES.items():
        write(out / f"{cid}.json", shaped(pattern, key, mid(cid), 1, "equipment"))
    for sid, (pattern, key) in SPOILER_RECIPES.items():
        write(out / f"{sid}.json", shaped(pattern, key, mid(sid), 1, "equipment"))

    write(out / "car_seat.json",
          shaped(["_L_", "LWL", "SSS"],
                 {"L": "minecraft:leather", "W": "#minecraft:wool", "S": S}, mid("car_seat")))
    write(out / "windshield.json",
          shaped(["GGG", "GGG", "SSS"], {"G": G, "S": S}, mid("windshield")))
    write(out / "headlight.json",
          shaped(["_G_", "GLG", "_S_"],
                 {"G": G, "L": "minecraft:glowstone_dust", "S": S}, mid("headlight"), 2))
    write(out / "wrench.json",
          shaped(["_SS", "_S_", "S__"], {"S": S}, mid("wrench")))
    write(out / "fuel_canister.json",
          shaped(["_S_", "CBC", "_S_"],
                 {"S": S, "C": "minecraft:coal", "B": "minecraft:bucket"}, mid("fuel_canister")))
    write(out / "car_manual.json",
          shapeless(["minecraft:book", "minecraft:iron_ingot"], mid("car_manual")))

    # --- sprey boyalar ---
    for color in spec.DYE_COLORS:
        write(out / f"{spec.spray_can_id(color)}.json",
              shapeless([S, "minecraft:iron_nugget", f"minecraft:{color}_dye"],
                        mid(spec.spray_can_id(color)), 2))

    # --- arabalar ---
    for car_id, car in spec.CARS.items():
        key = {
            "W": mid(car["wheel"]),
            "G": mid("windshield"),
            "E": mid(car["engine"]),
            "C": mid(car["chassis"]),
            "T": mid(car["trans"]),
            "S": mid("car_seat"),
        }
        write(out / f"{spec.car_item_id(car_id)}.json",
              shaped(["WGW", "ECT", "WSW"], key, mid(spec.car_item_id(car_id)), 1, "equipment"))

    # --- bloklar ---
    write(out / "assembly_table.json",
          shaped(["SSS", "PCP", "PPP"],
                 {"S": S, "P": "#minecraft:planks", "C": "minecraft:crafting_table"},
                 mid("assembly_table"), 1, "building"))
    write(out / "car_workbench.json",
          shaped(["SSS", "SAS", "III"],
                 {"S": S, "A": mid("assembly_table"), "I": "minecraft:iron_block"},
                 mid("car_workbench"), 1, "building"))
    write(out / "car_lift.json",
          shaped(["SSS", "_P_", "SIS"],
                 {"S": S, "P": "minecraft:piston", "I": "minecraft:iron_block"},
                 mid("car_lift"), 1, "building"))
    write(out / "fuel_pump.json",
          shaped(["SSS", "SBS", "SRS"],
                 {"S": S, "B": "minecraft:bucket", "R": "minecraft:redstone"},
                 mid("fuel_pump"), 1, "building"))
    write(out / "asphalt.json",
          shaped(["GGG", "GCG", "GGG"],
                 {"G": "minecraft:gravel", "C": "minecraft:coal_block"},
                 mid("asphalt"), 8, "building"))
    write(out / "asphalt_slab.json",
          shaped(["AAA"], {"A": mid("asphalt")}, mid("asphalt_slab"), 6, "building"))
    for name, dye in (("road_line_white", "white_dye"), ("road_line_yellow", "yellow_dye")):
        write(out / f"{name}.json",
              shaped(["AAA", "ADA", "AAA"],
                     {"A": mid("asphalt"), "D": f"minecraft:{dye}"}, mid(name), 8, "building"))


# ==========================================================================
# Item modelleri
# ==========================================================================

FLAT_ITEMS = (list(spec.MATERIALS) + list(spec.CHASSIS) + list(spec.ENGINES)
              + list(spec.TRANSMISSIONS) + list(spec.WHEELS)
              + [s for s in spec.SPOILERS if s != "spoiler_none"]
              + [spec.spray_can_id(c) for c in spec.DYE_COLORS]
              + [spec.car_item_id(c) for c in spec.CARS])

BLOCK_ITEMS = list(spec.BLOCKS)


def gen_item_models():
    for name in FLAT_ITEMS:
        write(ASSETS / f"models/item/{name}.json",
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": f"{NS}:item/{name}"}})
        write(ASSETS / f"items/{name}.json",
              {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

    for name in BLOCK_ITEMS:
        model = f"{NS}:block/{name}"
        if name == "asphalt_slab":
            model = f"{NS}:block/asphalt_slab"
        write(ASSETS / f"items/{name}.json",
              {"model": {"type": "minecraft:model", "model": model}})


# ==========================================================================
# Blok modelleri ve blockstate'ler
# ==========================================================================

def gen_blocks():
    b = ASSETS / "models/block"
    st = ASSETS / "blockstates"

    def tex(n):
        return f"{NS}:block/{n}"

    # --- asfalt ---
    write(b / "asphalt.json", {"parent": "minecraft:block/cube_all",
                               "textures": {"all": tex("asphalt")}})
    write(st / "asphalt.json", {"variants": {"": {"model": f"{NS}:block/asphalt"}}})

    # --- asfalt levha ---
    write(b / "asphalt_slab.json", {
        "parent": "minecraft:block/slab",
        "textures": {"bottom": tex("asphalt"), "top": tex("asphalt"), "side": tex("asphalt")}})
    write(b / "asphalt_slab_top.json", {
        "parent": "minecraft:block/slab_top",
        "textures": {"bottom": tex("asphalt"), "top": tex("asphalt"), "side": tex("asphalt")}})
    write(st / "asphalt_slab.json", {"variants": {
        "type=bottom": {"model": f"{NS}:block/asphalt_slab"},
        "type=top": {"model": f"{NS}:block/asphalt_slab_top"},
        "type=double": {"model": f"{NS}:block/asphalt"},
    }})

    # --- yol çizgileri: yatay eksene göre döner ---
    for name in ("road_line_white", "road_line_yellow"):
        write(b / f"{name}.json", {"parent": "minecraft:block/cube_all",
                                   "textures": {"all": tex(name)}})
        write(st / f"{name}.json", {"variants": {
            "axis=z": {"model": f"{NS}:block/{name}"},
            "axis=x": {"model": f"{NS}:block/{name}", "y": 90},
        }})

    # --- montaj tezgahı ---
    write(b / "assembly_table.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": tex("assembly_table_top"),
                     "side": tex("assembly_table_side"),
                     "bottom": tex("assembly_table_bottom")}})
    write(st / "assembly_table.json", {"variants": {"": {"model": f"{NS}:block/assembly_table"}}})

    # --- araba yapma masası ---
    write(b / "car_workbench.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": tex("car_workbench_top"),
                     "side": tex("car_workbench_side"),
                     "bottom": tex("assembly_table_bottom")}})
    write(st / "car_workbench.json", {"variants": {"": {"model": f"{NS}:block/car_workbench"}}})

    # --- araç lifti ---
    write(b / "car_lift.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": tex("car_lift_top"),
                     "side": tex("car_lift_side"),
                     "bottom": tex("assembly_table_bottom")}})
    write(st / "car_lift.json", {"variants": {"": {"model": f"{NS}:block/car_lift"}}})

    # --- yakıt pompası: baktığı yöne göre ön yüzü döner ---
    write(b / "fuel_pump.json", {
        "parent": "minecraft:block/orientable",
        "textures": {"top": tex("fuel_pump_top"),
                     "front": tex("fuel_pump_front"),
                     "side": tex("fuel_pump_side")}})
    write(st / "fuel_pump.json", {"variants": {
        "facing=north": {"model": f"{NS}:block/fuel_pump"},
        "facing=east": {"model": f"{NS}:block/fuel_pump", "y": 90},
        "facing=south": {"model": f"{NS}:block/fuel_pump", "y": 180},
        "facing=west": {"model": f"{NS}:block/fuel_pump", "y": 270},
    }})


# ==========================================================================
# Ganimet tabloları ve etiketler
# ==========================================================================

def gen_loot_and_tags():
    for name in spec.BLOCKS:
        write(DATA / f"loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": mid(name)}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    # Modun kendi sürüş etiketleri
    write(DATA / "tags/block/dusty.json", {"values": [
        "minecraft:sand", "minecraft:red_sand", "minecraft:suspicious_sand",
        "minecraft:gravel", "minecraft:suspicious_gravel",
        "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
        "minecraft:podzol", "minecraft:dirt_path", "minecraft:farmland",
        "minecraft:soul_sand", "minecraft:soul_soil", "minecraft:mud",
        "minecraft:packed_mud", "minecraft:grass_block",
    ]})
    write(DATA / "tags/block/high_grip.json", {"values": [
        mid("asphalt"), mid("asphalt_slab"), mid("road_line_white"), mid("road_line_yellow"),
        "minecraft:white_concrete",
        "minecraft:orange_concrete",
        "minecraft:magenta_concrete",
        "minecraft:light_blue_concrete",
        "minecraft:yellow_concrete",
        "minecraft:lime_concrete",
        "minecraft:pink_concrete",
        "minecraft:gray_concrete",
        "minecraft:light_gray_concrete",
        "minecraft:cyan_concrete",
        "minecraft:purple_concrete",
        "minecraft:blue_concrete",
        "minecraft:brown_concrete",
        "minecraft:green_concrete",
        "minecraft:red_concrete",
        "minecraft:black_concrete",
        "minecraft:stone", "minecraft:smooth_stone",
        "minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:deepslate",
        "minecraft:polished_andesite", "minecraft:polished_granite", "minecraft:polished_diorite",
        "minecraft:bricks", "minecraft:packed_ice",
    ]})
    write(DATA / "tags/block/low_grip.json", {"values": [
        "minecraft:ice", "minecraft:blue_ice", "minecraft:frosted_ice",
        "minecraft:snow_block", "minecraft:powder_snow", "minecraft:slime_block",
        "minecraft:soul_sand", "minecraft:honey_block",
    ]})

    # Kazma ile kırılabilirlik
    mine = RES / "data/minecraft/tags/block"
    write(mine / "mineable/pickaxe.json", {"values": [mid(n) for n in spec.BLOCKS]})
    write(mine / "needs_stone_tool.json", {"values": [mid("assembly_table"), mid("car_lift"),
                                                     mid("fuel_pump")]})


# ==========================================================================
# sounds.json
# ==========================================================================

def gen_sounds_json():
    obj = {}
    for sid, (tr_desc, _secs, loop) in spec.SOUNDS.items():
        obj[sid] = {
            "subtitle": f"subtitles.{NS}.{sid}",
            "sounds": [{"name": f"{NS}:{sid}", "stream": False}],
        }
    write(ASSETS / "sounds.json", obj)


# ==========================================================================
# Dil dosyaları
# ==========================================================================

SUBTITLES_TR = {
    "engine_idle": "Motor rölantide",
    "engine_loop_i4": "Motor çalışıyor",
    "engine_loop_i6": "Motor çalışıyor",
    "engine_loop_v6": "Motor çalışıyor",
    "engine_loop_v8": "Motor gürlüyor",
    "engine_loop_flat6": "Motor çalışıyor",
    "engine_start": "Motor çalıştırılıyor",
    "engine_stop": "Motor durduruluyor",
    "horn": "Korna",
    "brake_squeal": "Frenler gıcırdıyor",
    "gravel_loop": "Tekerlekler toprakta",
    "crash": "Araç çarpıyor",
    "door_close": "Kapı kapanıyor",
    "wrench_use": "Anahtar sesi",
}

SUBTITLES_EN = {
    "engine_idle": "Engine idles",
    "engine_loop_i4": "Engine runs",
    "engine_loop_i6": "Engine runs",
    "engine_loop_v6": "Engine runs",
    "engine_loop_v8": "Engine rumbles",
    "engine_loop_flat6": "Engine runs",
    "engine_start": "Engine starts",
    "engine_stop": "Engine stops",
    "horn": "Horn honks",
    "brake_squeal": "Brakes squeal",
    "gravel_loop": "Tyres on dirt",
    "crash": "Vehicle crashes",
    "door_close": "Door closes",
    "wrench_use": "Wrench clicks",
}

UI_TR = {
    f"itemGroup.{NS}.main": "RealCars",
    f"gui.{NS}.modification": "Araç Modifiye",
    f"gui.{NS}.assembly": "Montaj Tezgahı",
    f"gui.{NS}.workbench": "Araba Yapma Masası",
    f"gui.{NS}.slot.chassis": "Şasi",
    f"gui.{NS}.slot.engine": "Motor",
    f"gui.{NS}.slot.transmission": "Şanzıman",
    f"gui.{NS}.slot.wheels": "Tekerlek x4",
    f"gui.{NS}.slot.seat": "Koltuk",
    f"gui.{NS}.slot.windshield": "Ön Cam",
    f"gui.{NS}.tab.color": "Renk",
    f"gui.{NS}.tab.wheel": "Tekerlek",
    f"gui.{NS}.tab.spoiler": "Rüzgarlık",
    f"gui.{NS}.tab.engine": "Motor",
    f"gui.{NS}.apply": "Uygula",
    f"gui.{NS}.missing_part": "Gerekli parça envanterinde yok",
    f"gui.{NS}.installed": "Takılı",
    f"hud.{NS}.kmh": "km/s",
    f"hud.{NS}.gear": "Vites",
    f"hud.{NS}.gear.reverse": "R",
    f"hud.{NS}.gear.neutral": "N",
    f"hud.{NS}.fuel": "Yakıt",
    f"hud.{NS}.rpm": "d/dk",
    f"message.{NS}.no_fuel": "Yakıt bitti! Yakıt bidonu ile doldur.",
    f"message.{NS}.engine_on": "Motor çalıştırıldı",
    f"message.{NS}.engine_off": "Motor durduruldu",
    f"message.{NS}.refueled": "Araç yakıtı dolduruldu",
    f"message.{NS}.tank_full": "Depo zaten dolu",
    f"message.{NS}.locked": "Bu araç sana ait değil",
    f"message.{NS}.no_car_nearby": "Yakında araç yok — aracı bu bloğun yanına park et",
    f"message.{NS}.no_coal": "Envanterinde kömür yok",
    f"key.category.{NS}.controls": "RealCars",
    f"key.{NS}.engine_toggle": "Motoru çalıştır/durdur",
    f"key.{NS}.horn": "Korna",
    f"message.{NS}.help.title": "— Araba kullanımı —",
    f"message.{NS}.help.engine": "%s — motoru çalıştır / durdur (önce bunu yap)",
    f"message.{NS}.help.drive": "%s / %s — ileri / geri (ters yöne basmak fren yapar)",
    f"message.{NS}.help.steer": "%s / %s — direksiyon",
    f"message.{NS}.help.handbrake": "%s — el freni",
    f"message.{NS}.help.horn": "%s — korna",
    f"message.{NS}.help.dismount": "%s — araçtan in",
    f"message.{NS}.help.passenger": "Yolcu koltuğundasın: aracı ilk binen kişi sürer.",
    f"tooltip.{NS}.top_speed": "Azami hız: %s km/s",
    f"tooltip.{NS}.engine": "Motor: %s",
    f"tooltip.{NS}.transmission": "Şanzıman: %s",
    f"tooltip.{NS}.wheels": "Tekerlek: %s",
    f"tooltip.{NS}.fuel": "Yakıt: %s / %s L",
    f"tooltip.{NS}.color": "Renk",
    f"tooltip.{NS}.wrench_hint": "Araca sağ tıkla: modifiye et",
    f"tooltip.{NS}.canister_hint": "Araca sağ tıkla: yakıt doldur",
    f"entity.{NS}.car": "Araba",
    f"subtitles.{NS}.placeholder": "",
}

UI_EN = {
    f"itemGroup.{NS}.main": "RealCars",
    f"gui.{NS}.modification": "Vehicle Tuning",
    f"gui.{NS}.assembly": "Assembly Table",
    f"gui.{NS}.workbench": "Car Workbench",
    f"gui.{NS}.slot.chassis": "Chassis",
    f"gui.{NS}.slot.engine": "Engine",
    f"gui.{NS}.slot.transmission": "Gearbox",
    f"gui.{NS}.slot.wheels": "Wheels x4",
    f"gui.{NS}.slot.seat": "Seat",
    f"gui.{NS}.slot.windshield": "Windshield",
    f"gui.{NS}.tab.color": "Colour",
    f"gui.{NS}.tab.wheel": "Wheels",
    f"gui.{NS}.tab.spoiler": "Spoiler",
    f"gui.{NS}.tab.engine": "Engine",
    f"gui.{NS}.apply": "Apply",
    f"gui.{NS}.missing_part": "Required part not in inventory",
    f"gui.{NS}.installed": "Installed",
    f"hud.{NS}.kmh": "km/h",
    f"hud.{NS}.gear": "Gear",
    f"hud.{NS}.gear.reverse": "R",
    f"hud.{NS}.gear.neutral": "N",
    f"hud.{NS}.fuel": "Fuel",
    f"hud.{NS}.rpm": "rpm",
    f"message.{NS}.no_fuel": "Out of fuel! Refill with a fuel canister.",
    f"message.{NS}.engine_on": "Engine started",
    f"message.{NS}.engine_off": "Engine stopped",
    f"message.{NS}.refueled": "Vehicle refuelled",
    f"message.{NS}.tank_full": "Tank is already full",
    f"message.{NS}.locked": "This vehicle is not yours",
    f"message.{NS}.no_car_nearby": "No car nearby \u2014 park one next to this block",
    f"message.{NS}.no_coal": "No coal in your inventory",
    f"key.category.{NS}.controls": "RealCars",
    f"key.{NS}.engine_toggle": "Start/stop engine",
    f"key.{NS}.horn": "Horn",
    f"message.{NS}.help.title": "\u2014 Driving a car \u2014",
    f"message.{NS}.help.engine": "%s \u2014 start / stop the engine (do this first)",
    f"message.{NS}.help.drive": "%s / %s \u2014 forward / reverse (press the opposite way to brake)",
    f"message.{NS}.help.steer": "%s / %s \u2014 steer",
    f"message.{NS}.help.handbrake": "%s \u2014 handbrake",
    f"message.{NS}.help.horn": "%s \u2014 horn",
    f"message.{NS}.help.dismount": "%s \u2014 get out",
    f"message.{NS}.help.passenger": "You are in the passenger seat: whoever got in first drives.",
    f"tooltip.{NS}.top_speed": "Top speed: %s km/h",
    f"tooltip.{NS}.engine": "Engine: %s",
    f"tooltip.{NS}.transmission": "Gearbox: %s",
    f"tooltip.{NS}.wheels": "Wheels: %s",
    f"tooltip.{NS}.fuel": "Fuel: %s / %s L",
    f"tooltip.{NS}.color": "Colour",
    f"tooltip.{NS}.wrench_hint": "Right-click a car: tune it",
    f"tooltip.{NS}.canister_hint": "Right-click a car: refuel",
    f"entity.{NS}.car": "Car",
    f"subtitles.{NS}.placeholder": "",
}

DYE_NAMES_TR = {
    "white": "Beyaz", "orange": "Turuncu", "magenta": "Macenta", "light_blue": "Açık Mavi",
    "yellow": "Sarı", "lime": "Fıstık Yeşili", "pink": "Pembe", "gray": "Gri",
    "light_gray": "Açık Gri", "cyan": "Camgöbeği", "purple": "Mor", "blue": "Mavi",
    "brown": "Kahverengi", "green": "Yeşil", "red": "Kırmızı", "black": "Siyah",
}
DYE_NAMES_EN = {
    "white": "White", "orange": "Orange", "magenta": "Magenta", "light_blue": "Light Blue",
    "yellow": "Yellow", "lime": "Lime", "pink": "Pink", "gray": "Gray",
    "light_gray": "Light Gray", "cyan": "Cyan", "purple": "Purple", "blue": "Blue",
    "brown": "Brown", "green": "Green", "red": "Red", "black": "Black",
}


def gen_lang():
    for locale, idx, subs, ui, dyes in (
        ("tr_tr", 0, SUBTITLES_TR, UI_TR, DYE_NAMES_TR),
        ("en_us", 1, SUBTITLES_EN, UI_EN, DYE_NAMES_EN),
    ):
        out = dict(ui)
        for name, names in spec.MATERIALS.items():
            out[f"item.{NS}.{name}"] = names[idx]
        for name, names in spec.CHASSIS.items():
            out[f"item.{NS}.{name}"] = names[idx]
        for name, v in spec.ENGINES.items():
            out[f"item.{NS}.{name}"] = v[1 + idx]
        for name, v in spec.TRANSMISSIONS.items():
            out[f"item.{NS}.{name}"] = v[idx]
        for name, v in spec.WHEELS.items():
            out[f"item.{NS}.{name}"] = v[idx]
        for name, v in spec.SPOILERS.items():
            if name != "spoiler_none":
                out[f"item.{NS}.{name}"] = v[idx]
            out[f"{NS}.spoiler.{name}"] = v[idx]
        for color, label in dyes.items():
            suffix = "Sprey Boya" if idx == 0 else "Spray Paint"
            out[f"item.{NS}.{spec.spray_can_id(color)}"] = f"{label} {suffix}"
            out[f"{NS}.color.{color}"] = label
        for car_id, car in spec.CARS.items():
            out[f"item.{NS}.{spec.car_item_id(car_id)}"] = car["tr" if idx == 0 else "en"]
            out[f"entity.{NS}.car_{car_id}"] = car["tr" if idx == 0 else "en"]
        for name, names in spec.BLOCKS.items():
            out[f"block.{NS}.{name}"] = names[idx]
        for sid, text in subs.items():
            out[f"subtitles.{NS}.{sid}"] = text
        out.pop(f"subtitles.{NS}.placeholder", None)
        write(ASSETS / f"lang/{locale}.json", dict(sorted(out.items())))


def main():
    gen_recipes()
    gen_item_models()
    gen_blocks()
    gen_loot_and_tags()
    gen_sounds_json()
    gen_lang()
    counts = {
        "tarif": len(list((DATA / "recipe").glob("*.json"))),
        "item modeli": len(list((ASSETS / "models/item").glob("*.json"))),
        "item tanımı": len(list((ASSETS / "items").glob("*.json"))),
        "blok modeli": len(list((ASSETS / "models/block").glob("*.json"))),
        "blockstate": len(list((ASSETS / "blockstates").glob("*.json"))),
        "ganimet": len(list((DATA / "loot_table/blocks").glob("*.json"))),
    }
    print(", ".join(f"{v} {k}" for k, v in counts.items()))


if __name__ == "__main__":
    main()
