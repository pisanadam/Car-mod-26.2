#!/usr/bin/env python3
"""RealCars kaynak doğrulayıcısı.

Kırık referansları derleme öncesinde yakalar:
  * her item için doku / model / item tanımı / dil anahtarı var mı
  * tarifler var olmayan bir mod item'ına atıf yapıyor mu
  * iki tarif aynı kalıba düşüp çakışıyor mu
  * sounds.json'daki her giriş için .ogg dosyası var mı
  * Java enum'ları spec.py ile aynı içeriğe sahip mi

Hata varsa 1 ile çıkar.
"""

import json
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import spec  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets" / spec.MOD_ID
DATA = RES / "data" / spec.MOD_ID
JAVA = ROOT / "src/main/java/com/pisanadam/realcars"
NS = spec.MOD_ID

errors = []
warnings = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        err(f"JSON okunamadı: {path.relative_to(ROOT)} ({e})")
        return None


def check_json_parses():
    for p in list(RES.rglob("*.json")):
        load(p)


def check_items():
    """Her item için doku + model + item tanımı + dil anahtarı."""
    flat = (list(spec.MATERIALS) + list(spec.CHASSIS) + list(spec.ENGINES)
            + list(spec.TRANSMISSIONS) + list(spec.WHEELS)
            + [s for s in spec.SPOILERS if s != "spoiler_none"]
            + [spec.spray_can_id(c) for c in spec.DYE_COLORS]
            + [spec.car_item_id(c) for c in spec.CARS])

    tr = load(ASSETS / "lang/tr_tr.json") or {}
    en = load(ASSETS / "lang/en_us.json") or {}

    for name in flat:
        if not (ASSETS / f"textures/item/{name}.png").exists():
            err(f"doku eksik: textures/item/{name}.png")
        if not (ASSETS / f"models/item/{name}.json").exists():
            err(f"item modeli eksik: models/item/{name}.json")
        if not (ASSETS / f"items/{name}.json").exists():
            err(f"item tanımı eksik: items/{name}.json")
        for locale, table in (("tr_tr", tr), ("en_us", en)):
            if f"item.{NS}.{name}" not in table:
                err(f"dil anahtarı eksik ({locale}): item.{NS}.{name}")

    for name in spec.BLOCKS:
        if not (ASSETS / f"items/{name}.json").exists():
            err(f"blok item tanımı eksik: items/{name}.json")
        if not (ASSETS / f"blockstates/{name}.json").exists():
            err(f"blockstate eksik: blockstates/{name}.json")
        if not (DATA / f"loot_table/blocks/{name}.json").exists():
            err(f"ganimet tablosu eksik: loot_table/blocks/{name}.json")
        for locale, table in (("tr_tr", tr), ("en_us", en)):
            if f"block.{NS}.{name}" not in table:
                err(f"dil anahtarı eksik ({locale}): block.{NS}.{name}")

    if set(tr) != set(en):
        only_tr = sorted(set(tr) - set(en))
        only_en = sorted(set(en) - set(tr))
        if only_tr:
            err(f"en_us'ta eksik anahtarlar: {only_tr[:6]}")
        if only_en:
            err(f"tr_tr'de eksik anahtarlar: {only_en[:6]}")


def check_block_models():
    """Blok modellerinin atıf yaptığı dokular var mı."""
    for p in (ASSETS / "models/block").glob("*.json"):
        obj = load(p) or {}
        for key, ref in (obj.get("textures") or {}).items():
            if ref.startswith(f"{NS}:"):
                rel = ref.split(":", 1)[1]
                if not (ASSETS / f"textures/{rel}.png").exists():
                    err(f"{p.name}: '{key}' dokusu yok -> textures/{rel}.png")
    for p in (ASSETS / "blockstates").glob("*.json"):
        obj = load(p) or {}
        for variant, val in (obj.get("variants") or {}).items():
            entries = val if isinstance(val, list) else [val]
            for e in entries:
                model = e.get("model", "")
                if model.startswith(f"{NS}:"):
                    rel = model.split(":", 1)[1]
                    if not (ASSETS / f"models/{rel}.json").exists():
                        err(f"{p.name}[{variant}]: model yok -> models/{rel}.json")


def check_recipes():
    known = set(spec.all_item_ids())
    seen = {}
    for p in (DATA / "recipe").glob("*.json"):
        obj = load(p)
        if obj is None:
            continue
        refs = []
        if obj["type"] == "minecraft:crafting_shaped":
            refs = list(obj["key"].values())
            sig = (tuple(obj["pattern"]), tuple(sorted(obj["key"].items())))
            if sig in seen:
                err(f"tarif çakışması: {p.name} ile {seen[sig]} aynı kalıp ve girdilere sahip")
            seen[sig] = p.name
        else:
            refs = list(obj["ingredients"])
        refs.append(obj["result"]["id"])
        for r in refs:
            if r.startswith(f"{NS}:") and r.split(":", 1)[1] not in known:
                err(f"{p.name}: bilinmeyen item -> {r}")
            if r.startswith("#" + NS):
                err(f"{p.name}: modun kendi etiketi tarifte kullanılmış -> {r}")

    for car_id in spec.CARS:
        if not (DATA / f"recipe/{spec.car_item_id(car_id)}.json").exists():
            err(f"araba tarifi eksik: {car_id}")


def check_sounds():
    obj = load(ASSETS / "sounds.json") or {}
    if set(obj) != set(spec.SOUNDS):
        err(f"sounds.json spec ile uyuşmuyor: {sorted(set(obj) ^ set(spec.SOUNDS))}")
    tr = load(ASSETS / "lang/tr_tr.json") or {}
    for sid, entry in obj.items():
        for s in entry["sounds"]:
            rel = s["name"].split(":", 1)[1]
            if not (ASSETS / f"sounds/{rel}.ogg").exists():
                err(f"ses dosyası yok: sounds/{rel}.ogg ({sid})")
        if entry.get("subtitle") and entry["subtitle"] not in tr:
            err(f"altyazı anahtarı eksik: {entry['subtitle']}")


def check_java_enums():
    """Java enum sabitlerinin spec.py ile aynı olduğunu doğrular."""
    checks = [
        ("entity/EngineType.java", spec.ENGINES),
        ("entity/TransmissionType.java", spec.TRANSMISSIONS),
        ("entity/WheelType.java", spec.WHEELS),
        ("entity/SpoilerType.java", spec.SPOILERS),
        ("entity/CarModel.java", {spec.car_item_id(c): None for c in spec.CARS}),
    ]
    for rel, table in checks:
        path = JAVA / rel
        if not path.exists():
            warn(f"Java dosyası henüz yok, atlandı: {rel}")
            continue
        text = path.read_text(encoding="utf-8")
        # enum sabitleri: satır başında BÜYÜK_HARF ve ardından '(' ya da ',' / ';'
        found = set(re.findall(r"^\t([A-Z][A-Z0-9_]*)\s*\(", text, re.M))
        expected = {name.upper() for name in table}
        missing = expected - found
        extra = found - expected
        if missing:
            err(f"{rel}: spec'te olup Java'da olmayan sabitler: {sorted(missing)}")
        if extra:
            err(f"{rel}: Java'da olup spec'te olmayan sabitler: {sorted(extra)}")


def main():
    check_json_parses()
    check_items()
    check_block_models()
    check_recipes()
    check_sounds()
    check_java_enums()

    for w in warnings:
        print(f"  uyarı: {w}")
    if errors:
        print(f"\n{len(errors)} hata:")
        for e in errors:
            print(f"  HATA: {e}")
        return 1
    print(f"Doğrulama başarılı — {len(spec.CARS)} araç, {len(spec.all_item_ids())} item, "
          f"{len(spec.SOUNDS)} ses.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
