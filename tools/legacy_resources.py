"""Eski Minecraft sürümleri için veri dosyalarını ortak kaynaklardan türetir.

Ortak kaynaklar (src/main/resources) en yeni biçimdedir. Eski sürümler bazı
dosyaları başka biçimde ister; bu betik onları versions/<sürüm>/src/main/resources
altına yazar ve derleme orada aynı yoldaki ortak dosyanın yerine onları koyar.

  1.21.1 : tariflerde malzemeler nesne ({"item": ...} / {"tag": ...});
           eşya modeli models/item altında olmalı (items/ tanımları 1.21.4+).
  1.20.1 : üstüne klasör adları çoğul (recipes, loot_tables, tags/blocks),
           tarif sonucu {"item": ...}.

Kullanım: python3 tools/legacy_resources.py 1.21.1 1.20.1
"""

import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / "src/main/resources"


def ingredient(value):
    if isinstance(value, str):
        return {"tag": value[1:]} if value.startswith("#") else {"item": value}
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    return value


def convert_recipe(recipe, mc):
    out = dict(recipe)
    if "key" in out:
        out["key"] = {k: ingredient(v) for k, v in out["key"].items()}
    if "ingredients" in out:
        out["ingredients"] = [ingredient(v) for v in out["ingredients"]]
    if "ingredient" in out:
        out["ingredient"] = ingredient(out["ingredient"])
    if mc == "1.20.1" and isinstance(out.get("result"), dict):
        result = dict(out["result"])
        result["item"] = result.pop("id")
        out["result"] = result
    return out


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def generate(mc):
    out = ROOT / "versions" / mc / "src/main/resources"
    plural = mc == "1.20.1"
    for sub in ("data", "assets/realcars/models/item"):
        target = out / sub
        if target.exists():
            shutil.rmtree(target)

    # Tarifler
    recipe_dir = "recipes" if plural else "recipe"
    for src in sorted((SHARED / "data/realcars/recipe").glob("*.json")):
        recipe = json.loads(src.read_text(encoding="utf-8"))
        write(out / "data/realcars" / recipe_dir / src.name, convert_recipe(recipe, mc))

    # Yağma tabloları ve etiketler: içerik aynı, yalnızca klasör adları değişir.
    if plural:
        renames = {"loot_table": "loot_tables", "tags/block": "tags/blocks", "tags/item": "tags/items"}
        for src in sorted((SHARED / "data").rglob("*.json")):
            rel = src.relative_to(SHARED / "data").as_posix()
            if "/recipe/" in rel:
                continue
            for old, new in renames.items():
                rel = rel.replace(f"/{old}/", f"/{new}/")
            write(out / "data" / rel, json.loads(src.read_text(encoding="utf-8")))

    # Eşya modelleri: items/ tanımının gösterdiği model; eksikse ona bağlanan bir model yazılır.
    models = SHARED / "assets/realcars/models/item"
    for src in sorted((SHARED / "assets/realcars/items").glob("*.json")):
        if (models / src.name).exists():
            continue
        model = json.loads(src.read_text(encoding="utf-8"))["model"]
        if model.get("type") != "minecraft:model":
            raise SystemExit(f"{src.name}: düz model değil, elle çevrilmeli")
        write(out / "assets/realcars/models/item" / src.name, {"parent": model["model"]})

    print(mc, "->", out.relative_to(ROOT))


if __name__ == "__main__":
    for version in sys.argv[1:] or ["1.21.1", "1.20.1"]:
        generate(version)
