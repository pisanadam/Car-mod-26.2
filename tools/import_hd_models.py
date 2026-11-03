#!/usr/bin/env python3
"""Açık lisanslı yüksek poligonlu araç modellerini mod formatına çevirir.

Kaynak: `hd_models.py` tablosundaki her araç için Objaverse'ten (Sketchfab
modellerinin lisanslı arşivi) GLB indirilir ve şunlar üretilir:

  src/main/resources/assets/realcars/hd/<id>.rcm
      gzip'li ikili mesh: parçalar (boyanan gövde, boyasız gövde, cam, far,
      stop, dört tekerlek), her parça için iki detay seviyesi (yakın / uzak)
  src/main/resources/assets/realcars/textures/entity/car/hd/<id>.png
      modelin bütün dokularını ve düz renklerini tek sayfada toplayan atlas

Minecraft yalnızca dörtgen çizer; üçgenler komşularıyla eşlenip dörtgene
çevrilir, eşi bulunamayanlar son köşesi tekrarlanan dörtgen olur.

Tekerlekler ayrı dosya düğümü olarak gelmez, geometriden bulunur: her
köşede yere değen lastiğin ortası ve en çok açısal kapsama veren çember
yarıçapı aranır; o silindirin içinde kalan üçgenler tekerlek sayılır.

Kullanım:
    pip install trimesh pymeshlab numpy Pillow
    python3 tools/import_hd_models.py            # hepsi
    python3 tools/import_hd_models.py golf_gti   # tek araç
    python3 tools/import_hd_models.py --preview  # ayrıca önizleme GLB'si
"""

import gzip
import io
import json
import math
import pathlib
import re
import struct
import sys
import urllib.request

import numpy as np

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import hd_models  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".hd_cache"
RES = ROOT / "src" / "main" / "resources" / "assets" / "realcars"
OUT_MESH = RES / "hd"
OUT_TEX = RES / "textures" / "entity" / "car" / "hd"
CAR_MODEL_JAVA = ROOT / "src" / "main" / "java" / "com" / "pisanadam" / "realcars" / "entity" / "CarModel.java"
OBJAVERSE = "https://huggingface.co/datasets/allenai/objaverse/resolve/main/"

# Detay bütçesi (üçgen). Yakın görünüm "yüksek poligon"; uzak görünüm 40
# bloktan ötesi için, orada fark edilmez ama kare hızını korur.
LOD_BUDGETS = tuple(int(x) for x in __import__("os").environ.get("RCM_BUDGETS", "70000,9000").split(","))
PROFILE_BINS = 48
VERBOSE = bool(__import__("os").environ.get("RCM_VERBOSE"))

KIND_PAINT, KIND_PLAIN, KIND_GLASS, KIND_LIGHT_FRONT, KIND_LIGHT_REAR, KIND_WHEEL = range(6)
KIND_DROP = -1
KIND_LIGHT = 99  # sınıflandırma ara değeri, konuma göre ön/arkaya ayrılır

FORMAT_MAGIC = b"RCM1"
FORMAT_VERSION = 1


# ---------------------------------------------------------------------------
# Yardımcılar
# ---------------------------------------------------------------------------

def srgb_to_linear(c):
    c = np.asarray(c, dtype=np.float64)
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def linear_to_srgb(c):
    c = np.clip(np.asarray(c, dtype=np.float64), 0.0, 1.0)
    return np.where(c <= 0.0031308, c * 12.92, 1.055 * np.power(c, 1.0 / 2.4) - 0.055)


def matches(name, patterns):
    """Büyük/küçük harf duyarsız alt dizgi eşleşmesi; "=" ile başlayan desen
    tam eşleşme ister ("=material", "material_1" ile eşleşmez)."""
    low = (name or "").lower()
    for p in patterns:
        p = p.lower()
        if p.startswith("="):
            if low == p[1:]:
                return True
        elif p in low:
            return True
    return False


def car_bodies():
    """CarModel.java'dan araç boyu/genişliği (piksel, 16 = 1 blok)."""
    text = CAR_MODEL_JAVA.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'CAR_[A-Z0-9_]+\("car_([a-z0-9_]+)".*?Body\.of\(Silhouette\.\w+,\s*([\d.]+)F,\s*([\d.]+)F\)',
                         text, re.S):
        out[m.group(1)] = dict(length=float(m.group(2)), width=float(m.group(3)))
    return out


def download(uid):
    CACHE.mkdir(parents=True, exist_ok=True)
    dst = CACHE / (uid + ".glb")
    if dst.exists() and dst.stat().st_size > 0:
        return dst
    index_path = CACHE / "object-paths.json.gz"
    if not index_path.exists():
        print("  Objaverse dizini indiriliyor...")
        index_path.write_bytes(urllib.request.urlopen(OBJAVERSE + "object-paths.json.gz", timeout=300).read())
    paths = json.loads(gzip.decompress(index_path.read_bytes()))
    print("  indiriliyor:", paths[uid])
    dst.write_bytes(urllib.request.urlopen(OBJAVERSE + paths[uid], timeout=600).read())
    return dst


# ---------------------------------------------------------------------------
# Yükleme
# ---------------------------------------------------------------------------

class Material:
    def __init__(self, name, factor, image, blend):
        self.name = name or "unnamed"
        self.factor = factor            # doğrusal RGBA, 0..1
        self.image = image              # PIL RGBA ya da None
        self.blend = blend
        self._avg = None

    def average_srgb(self):
        """Malzemenin ortalama rengi (sRGB, 0..1) ve opaklığı."""
        if self._avg is None:
            rgb = linear_to_srgb(self.factor[:3])
            alpha = self.factor[3]
            if self.image is not None:
                small = np.asarray(self.image.resize((16, 16)), dtype=np.float64) / 255.0
                tex = srgb_to_linear(small[..., :3]).reshape(-1, 3).mean(0)
                rgb = linear_to_srgb(tex * self.factor[:3])
                alpha = alpha * small[..., 3].mean()
            self._avg = (rgb, alpha)
        return self._avg


def load_glb(path):
    import trimesh

    scene = trimesh.load(str(path), force="scene", process=False)
    materials = {}
    chunks = []
    for node in scene.graph.nodes_geometry:
        transform, geom_name = scene.graph[node]
        mesh = scene.geometry[geom_name]
        if not isinstance(mesh, trimesh.Trimesh) or len(mesh.faces) == 0:
            continue
        visual = mesh.visual
        mat = getattr(visual, "material", None)
        uv = getattr(visual, "uv", None)
        key = id(mat) if mat is not None else ("color", geom_name)
        if key not in materials:
            materials[key] = make_material(mat, visual)
        verts = np.asarray(mesh.vertices, dtype=np.float64)
        rot = transform[:3, :3]
        verts = verts @ rot.T + transform[:3, 3]
        faces = np.asarray(mesh.faces, dtype=np.int64)
        if np.linalg.det(rot) < 0:
            faces = faces[:, ::-1]
        if uv is None or len(uv) != len(mesh.vertices):
            uv = np.zeros((len(verts), 2))
        else:
            # trimesh UV'yi OpenGL düzenine (v yukarı) çevirerek okur; atlas ve
            # Minecraft resim satır düzeninde (v aşağı) çalışır.
            uv = np.column_stack([uv[:, 0], 1.0 - uv[:, 1]])
        chunks.append(dict(node=str(node), mat=materials[key], v=verts, f=faces,
                           uv=np.asarray(uv, dtype=np.float64)))
    return chunks


def make_material(mat, visual):
    from PIL import Image

    if mat is None:
        # Yalnızca köşe rengi taşıyan mesh: ortalama rengi düz renk say.
        colors = getattr(visual, "vertex_colors", None)
        rgba = np.array([200, 200, 200, 255]) if colors is None else np.asarray(colors)[:, :4].mean(0)
        return Material("vertex_color", np.concatenate([srgb_to_linear(rgba[:3] / 255.0), [rgba[3] / 255.0]]),
                        None, False)
    name = getattr(mat, "name", None)
    factor = getattr(mat, "baseColorFactor", None)
    if factor is None:
        factor = np.array([1.0, 1.0, 1.0, 1.0])
    else:
        factor = np.asarray(factor, dtype=np.float64)
        if factor.max() > 1.0:
            factor = factor / 255.0
    image = getattr(mat, "baseColorTexture", None)
    if image is None and hasattr(mat, "image"):
        image = mat.image
        diffuse = getattr(mat, "diffuse", None)
        if diffuse is not None:
            factor = np.concatenate([srgb_to_linear(np.asarray(diffuse[:3]) / 255.0), [diffuse[3] / 255.0]])
    if image is not None:
        image = image.convert("RGBA")
        if max(image.size) > 1024:
            image = image.resize((min(1024, image.size[0]), min(1024, image.size[1])), Image.LANCZOS)
    blend = str(getattr(mat, "alphaMode", "") or "").upper() == "BLEND"
    return Material(name, factor, image, blend)


# ---------------------------------------------------------------------------
# Sınıflandırma
# ---------------------------------------------------------------------------

def classify_chunk(chunk, cfg):
    """Her üçgen için tür: boya / boyasız / cam / ışık / at."""
    mat = chunk["mat"]
    name = mat.name
    n = len(chunk["f"])
    if matches(name, cfg.get("drop", [])) or matches(chunk["node"], cfg.get("drop", [])):
        return np.full(n, KIND_DROP)
    if matches(name, cfg.get("lights", [])) or matches(chunk["node"], cfg.get("light_nodes", [])):
        return np.full(n, KIND_LIGHT)
    if matches(name, cfg.get("glass", [])):
        return np.full(n, KIND_GLASS)
    if matches(name, cfg.get("paint", [])) or matches(chunk["node"], cfg.get("paint_nodes", [])):
        return np.full(n, KIND_PAINT)
    if mat.blend and mat.average_srgb()[1] < 0.75 and not matches(name, cfg.get("opaque", [])):
        return np.full(n, KIND_GLASS)
    kinds = np.full(n, KIND_PLAIN)
    by_color = cfg.get("paint_color")
    if by_color is not None and mat.image is not None and (
            not by_color.get("materials") or matches(name, by_color["materials"])):
        kinds[paint_faces_by_color(chunk, by_color)] = KIND_PAINT
    return kinds


def sample_face_colors(chunk):
    """Her üçgenin ağırlık merkezinde dokudan okunan sRGB renk (0..1)."""
    mat = chunk["mat"]
    img = np.asarray(mat.image, dtype=np.float64) / 255.0
    h, w = img.shape[:2]
    uv = chunk["uv"][chunk["f"]]  # (n,3,2)
    pts = np.concatenate([uv.mean(1)[:, None, :], uv * 0.6 + uv.mean(1)[:, None, :] * 0.4], axis=1)
    u = np.mod(pts[..., 0], 1.0)
    v = np.mod(pts[..., 1], 1.0)
    px = np.clip((u * w).astype(int), 0, w - 1)
    py = np.clip((v * h).astype(int), 0, h - 1)
    return img[py, px, :3].mean(1)


def paint_faces_by_color(chunk, rule):
    return color_mask(sample_face_colors(chunk), rule)


def color_mask(colors, rule):
    """Renkleri (sRGB, 0..1, son eksen RGB) boya rengine yakınlığa göre seçer."""
    ref = np.asarray(rule["color"], dtype=np.float64) / 255.0
    # Renk tonu yakınlığı: parlaklıktan bağımsız olsun diye renkler
    # kendi parlaklıklarına bölünerek karşılaştırılır.
    def chroma(c):
        lum = c.max(axis=-1, keepdims=True)
        return c / np.maximum(lum, 1e-3), lum[..., 0]
    cc, lum = chroma(colors)
    rc, rlum = chroma(ref[None, :])
    dist = np.linalg.norm(cc - rc, axis=-1)
    tol = rule.get("tol", 0.18)
    lo, hi = rule.get("lum", (0.35, 3.0))
    return (dist < tol) & (lum > rlum[0] * lo) & (lum < rlum[0] * hi)


# ---------------------------------------------------------------------------
# Yön / ölçek / tekerlek
# ---------------------------------------------------------------------------

def rotation_y(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _axle_candidates(q, ground, length, height):
    """Bir köşedeki noktalar için (yarıçap -> (puan, z merkezi)) tablosu."""
    contact = q[q[:, 1] < ground + 0.012 * length]
    if len(contact) < 3:
        contact = q[q[:, 1] < ground + 0.03 * length]
    if len(contact) < 3:
        return None
    zc = float(np.median(contact[:, 2]))
    xs = np.sort(np.abs(contact[:, 0]))
    x_in, x_out = float(xs[int(len(xs) * 0.02)]), float(xs[max(int(len(xs) * 0.98) - 1, 0)])
    band = q[(np.abs(q[:, 0]) > x_in - 0.01 * length) & (np.abs(q[:, 0]) < x_out + 0.01 * length)
             & (np.abs(q[:, 2] - zc) < 0.2 * length)]
    table = {}
    for r in np.arange(0.045, min(0.13, 0.32 * height / length), 0.0015) * length:
        best = (0, zc)
        for dz in np.linspace(-0.03, 0.03, 13) * length:
            cy, cz = ground + r, zc + dz
            d = np.hypot(band[:, 1] - cy, band[:, 2] - cz)
            on = band[np.abs(d - r) < max(0.012 * r, 0.0025 * length)]
            if len(on) < 8:
                continue
            ang = np.arctan2(on[:, 1] - cy, on[:, 2] - cz)
            bins = np.unique(((ang + math.pi) / (2 * math.pi) * 36).astype(int) % 36)
            # Lastik tam bir çember çizer; çamurluk kemeri yalnızca üst yarıyı.
            lower = np.count_nonzero(bins < 18)
            score = len(bins) + lower
            if score > best[0]:
                best = (score, cz)
        table[round(float(r), 5)] = best
    return dict(table=table, x_in=x_in, x_out=x_out)


def detect_wheels(v, length):
    """Dört tekerleğin merkezini, yarıçapını ve iç/dış yüzünü bulur.

    Her köşede yere değen lastiğin ortası bulunur, sonra çember yarıçapı
    açısal kapsamaya göre seçilir; aynı aksın iki tekerleği ortak yarıçap
    kullanır (model simetrik olmasa da tekerlekler aynıdır)."""
    ground = v[:, 1].min()
    height = v[:, 1].max() - ground
    wheels = []
    for front in (True, False):
        cands = []
        for left in (True, False):
            sel = ((v[:, 2] < 0) == front) & ((v[:, 0] < 0) == left)
            c = _axle_candidates(v[sel], ground, length, height)
            if c is None:
                raise RuntimeError("tekerlek bulunamadı (%s)" % ("ön" if front else "arka"))
            cands.append((left, c))
        radii = sorted(set(cands[0][1]["table"]) & set(cands[1][1]["table"]))
        r = max(radii, key=lambda rr: (sum(c["table"][rr][0] for _, c in cands), rr))
        for left, c in cands:
            cz = c["table"][r][1]
            sign = -1.0 if left else 1.0
            width = max(c["x_out"] - c["x_in"], 0.04 * length)
            wheels.append(dict(front=front, left=left, r=float(r), cy=float(ground + r), cz=float(cz),
                               x_in=c["x_in"], x_out=c["x_out"], sign=sign, width=width,
                               cx=float(sign * (c["x_in"] + c["x_out"]) / 2)))
    # Aynı aksın iki tekerleği aynı z'de olmalı.
    for front in (True, False):
        pair = [w for w in wheels if w["front"] == front]
        z = float(np.mean([w["cz"] for w in pair]))
        for w in pair:
            w["cz"] = z
    return wheels


def drop_shadow_planes(chunks, length):
    """Bazı modellerin altında sahte gölge için büyük, yatay bir düzlem olur;
    araç onun üstüne otururdu. Zemine yapışık, aracın büyük bölümünü kaplayan
    yatay yüzler atılır."""
    ground = min(c["v"][c["f"]].reshape(-1, 3)[:, 1].min() for c in chunks)
    for ch in chunks:
        tri = ch["v"][ch["f"]]
        low = (tri[..., 1] < ground + 0.01 * length).all(axis=1)
        span = tri[..., [0, 2]].max(axis=1) - tri[..., [0, 2]].min(axis=1)
        big = span.max(axis=1) > 0.3 * length
        bad = low & big
        if bad.any():
            print("    zemin düzlemi atıldı: %s (%d üçgen)" % (ch["mat"].name, int(bad.sum())))
            ch["f"], ch["kind"] = ch["f"][~bad], ch["kind"][~bad]


def wheel_face_mask(v, f, wheel):
    corners = v[f]  # (n,3,3)
    ax = np.abs(corners[..., 0])
    side = np.sign(corners[..., 0]) == wheel["sign"]
    rad = np.hypot(corners[..., 1] - wheel["cy"], corners[..., 2] - wheel["cz"])
    inner = wheel["x_in"] - 0.45 * wheel["width"]
    outer = wheel["x_out"] + 0.25 * wheel["width"]
    inside = side & (rad <= wheel["r"] * 1.03) & (ax >= inner) & (ax <= outer)
    return inside.all(axis=1)


# ---------------------------------------------------------------------------
# Sadeleştirme, normaller, dörtgenler
# ---------------------------------------------------------------------------

def weld(v, f, uv):
    """Aynı konum + UV'deki köşeleri birleştirir (sadeleştirme için şart)."""
    key = np.concatenate([np.round(v[f.reshape(-1)] * 1e5), np.round(uv[f.reshape(-1)] * 1e4)], axis=1)
    uniq, inverse = np.unique(key, axis=0, return_inverse=True)
    flat = f.reshape(-1)
    first = np.zeros(len(uniq), dtype=np.int64)
    first[inverse[::-1]] = flat[::-1]
    nf = inverse.reshape(-1, 3)
    keep = (nf[:, 0] != nf[:, 1]) & (nf[:, 1] != nf[:, 2]) & (nf[:, 0] != nf[:, 2])
    return v[first], nf[keep], uv[first], keep


def simplify(v, f, uv, ratio, textured):
    """Quadric kenar çökertmeyle sadeleştirir (MeshLab). Yüz normali ters
    dönecek çökertmeler reddedilir: ince, çift yüzlü kapı panellerinde dış
    yüzün içe göçüp boyanın kaybolmasını bu engeller.

    UV hem girişte hem çıkışta köşe başınadır (n, 3, 2)."""
    if ratio >= 0.999 or len(f) < 300:
        return v, f, uv
    target = max(96, int(len(f) * ratio))
    result = run_isolated(_meshlab_decimate, v, f, uv, target, textured)
    if textured and (result is None or len(result[1]) > max(target * 1.5, target + 200)):
        # Dokulu çökertme başarısız (ya da hiç ilerlemedi): geometriyi
        # dokusuz sadeleştir, UV'yi en yakın özgün köşeden al.
        plain = run_isolated(_meshlab_decimate, v, f, uv, target, False)
        if plain is not None:
            from scipy.spatial import cKDTree

            nv, nf, _ = plain
            corner_pos = v[f].reshape(-1, 3)
            _, nearest = cKDTree(corner_pos).query(nv[nf].reshape(-1, 3))
            result = (nv, nf, uv.reshape(-1, 2)[nearest].reshape(-1, 3, 2))
    if result is None:
        print("    MeshLab bu parçada düştü; parça sadeleştirilmeden bırakıldı (%d üçgen)" % len(f))
        return v, f, uv
    return result


def run_isolated(fn, *args):
    """fn'i çatallanmış bir alt süreçte çalıştırır; alt süreç çökerse None.
    MeshLab bazı bozuk geometride C++ tarafında segfault veriyor."""
    import os
    import pickle

    read_fd, write_fd = os.pipe()
    pid = os.fork()
    if pid == 0:
        os.close(read_fd)
        try:
            data = pickle.dumps(fn(*args))
        except Exception:
            data = pickle.dumps(None)
        with os.fdopen(write_fd, "wb") as out:
            out.write(data)
        os._exit(0)
    os.close(write_fd)
    with os.fdopen(read_fd, "rb") as inp:
        data = inp.read()
    _, status = os.waitpid(pid, 0)
    if status != 0 or not data:
        return None
    return pickle.loads(data)


def _meshlab_decimate(v, f, uv, target, textured):
    import pymeshlab

    ms = pymeshlab.MeshSet()
    v = np.ascontiguousarray(v, dtype=np.float64)
    f = np.ascontiguousarray(f, dtype=np.int32)
    if textured:
        # Köşe başı UV'nin bir doku indisine bağlı olması gerekiyor; numpy'dan
        # kurulan mesh'te bu indis boş kalıyor. En kısa yol, mesh'i bir doku
        # malzemesiyle OBJ olarak yazıp MeshLab'e öyle okutmak.
        import tempfile

        with tempfile.TemporaryDirectory() as tmp:
            tmp = pathlib.Path(tmp)
            from PIL import Image

            Image.new("RGB", (4, 4)).save(tmp / "t.png")
            (tmp / "t.mtl").write_text("newmtl m\nmap_Kd t.png\n")
            corners = uv.reshape(-1, 2)
            idx = np.arange(len(f) * 3).reshape(-1, 3) + 1
            with open(tmp / "t.obj", "w") as out:
                out.write("mtllib t.mtl\nusemtl m\n")
                np.savetxt(out, v, fmt="v %.7f %.7f %.7f")
                np.savetxt(out, corners, fmt="vt %.7f %.7f")
                np.savetxt(out, np.column_stack([f[:, 0] + 1, idx[:, 0], f[:, 1] + 1, idx[:, 1],
                                                 f[:, 2] + 1, idx[:, 2]]), fmt="f %d/%d %d/%d %d/%d")
            ms.load_new_mesh(str(tmp / "t.obj"))
        ms.apply_filter("meshing_remove_null_faces")
        ms.apply_filter("meshing_decimation_quadric_edge_collapse_with_texture", targetfacenum=target,
                        qualitythr=0.3, preserveboundary=False, optimalplacement=True,
                        preservenormal=True, planarquadric=True)
    else:
        ms.add_mesh(pymeshlab.Mesh(vertex_matrix=v, face_matrix=f))
        # Çift ve sıfır alanlı yüzler çökertme sırasında MeshLab'i düşürüyor.
        ms.apply_filter("meshing_remove_duplicate_faces")
        ms.apply_filter("meshing_remove_null_faces")
        ms.apply_filter("meshing_remove_unreferenced_vertices")
        ms.apply_filter("meshing_decimation_quadric_edge_collapse", targetfacenum=target, qualitythr=0.3,
                        preserveboundary=True, boundaryweight=1.5, preservenormal=True, optimalplacement=True,
                        planarquadric=True)
    m = ms.current_mesh()
    nv = np.asarray(m.vertex_matrix(), dtype=np.float64)
    nf = np.asarray(m.face_matrix(), dtype=np.int64)
    if textured:
        cuv = np.asarray(m.wedge_tex_coord_matrix(), dtype=np.float64).reshape(-1, 3, 2)
    else:
        cuv = np.zeros((len(nf), 3, 2))
    return nv, nf, cuv


def face_normals(v, f):
    n = np.cross(v[f[:, 1]] - v[f[:, 0]], v[f[:, 2]] - v[f[:, 0]])
    area = np.linalg.norm(n, axis=1, keepdims=True)
    return n / np.maximum(area, 1e-12), area[:, 0]


def corner_normals(v, f, crease_deg=55.0):
    """Köşe başına normal: komşu yüzlerin ağırlıklı ortalaması, ama keskin
    kenarlarda (açı > crease) yüzün kendi normali korunur."""
    fn, area = face_normals(v, f)
    vn = np.zeros_like(v)
    np.add.at(vn, f.reshape(-1), np.repeat(fn * area[:, None], 3, axis=0))
    vn /= np.maximum(np.linalg.norm(vn, axis=1, keepdims=True), 1e-12)
    cn = vn[f]  # (n,3,3)
    cos = (cn * fn[:, None, :]).sum(-1)
    sharp = cos < math.cos(math.radians(crease_deg))
    cn[sharp] = np.repeat(fn[:, None, :], 3, axis=1)[sharp]
    return cn


def pair_quads(f, v):
    """Kenar paylaşan, aynı düzlemdeki üçgen çiftlerini dörtgene birleştirir."""
    fn, _ = face_normals(v, f)
    edge_owner = {}
    for i, (a, b, c) in enumerate(f.tolist()):
        for e0, e1 in ((a, b), (b, c), (c, a)):
            edge_owner.setdefault((e0, e1), i)
    used = np.zeros(len(f), dtype=bool)
    quads = []
    fl = f.tolist()
    for i, (a, b, c) in enumerate(fl):
        if used[i]:
            continue
        done = False
        for (p, q, r) in ((a, b, c), (b, c, a), (c, a, b)):
            # Kenar q->r'yi ters yönde (r->q) içeren üçgeni ara.
            j = edge_owner.get((r, q))
            if j is None or j == i or used[j]:
                continue
            tri = fl[j]
            d = [x for x in tri if x != q and x != r]
            if len(d) != 1:
                continue
            d = d[0]
            if float(np.dot(fn[i], fn[j])) < 0.985:
                continue
            quad = (p, q, d, r)
            if not convex(v, quad):
                continue
            used[i] = used[j] = True
            quads.append(quad)
            done = True
            break
        if not done:
            used[i] = True
            quads.append((a, b, c, c))
    return np.array(quads, dtype=np.int64).reshape(-1, 4)


def convex(v, quad):
    p = v[list(quad)]
    n = None
    for k in range(4):
        e1 = p[(k + 1) % 4] - p[k]
        e2 = p[(k + 2) % 4] - p[(k + 1) % 4]
        c = np.cross(e1, e2)
        if n is None:
            n = c
        elif np.dot(n, c) <= 0:
            return False
    return True


# ---------------------------------------------------------------------------
# Doku atlası
# ---------------------------------------------------------------------------

class Atlas:
    PAD = 2

    def __init__(self):
        self.cells = {}   # key -> dict(image, x, y)

    def add(self, key, image):
        if key not in self.cells:
            self.cells[key] = dict(image=image)

    def pack(self):
        from PIL import Image

        for max_side in (512, 384, 256, 192, 128, 96, 64):
            size = self._try_pack(max_side)
            if size is not None:
                break
        else:
            raise RuntimeError("atlas sığmadı")
        atlas = Image.new("RGBA", size, (0, 0, 0, 0))
        for cell in self.cells.values():
            img = cell["scaled"]
            x, y = cell["x"], cell["y"]
            pad = self.PAD
            # Kenar piksellerini dışarı uzat: filtrelemede komşu hücre sızmasın.
            padded = Image.new("RGBA", (img.width + 2 * pad, img.height + 2 * pad))
            padded.paste(img.resize((img.width + 2 * pad, img.height + 2 * pad), Image.NEAREST), (0, 0))
            padded.paste(img, (pad, pad))
            atlas.paste(padded, (x - pad, y - pad))
        self.size = size
        return atlas

    def _try_pack(self, max_side):
        from PIL import Image

        items = []
        for key, cell in self.cells.items():
            img = cell["image"]
            scale = min(1.0, max_side / max(img.size))
            w, h = max(4, int(round(img.width * scale))), max(4, int(round(img.height * scale)))
            cell["scaled"] = img if (w, h) == img.size else img.resize((w, h), Image.LANCZOS)
            items.append((h, w, key))
        items.sort(key=lambda it: (it[0], it[1]), reverse=True)
        for side in (512, 1024, 2048):
            x = y = row_h = 0
            ok = True
            for h, w, key in items:
                cw, ch = w + 2 * self.PAD, h + 2 * self.PAD
                if x + cw > side:
                    x, y, row_h = 0, y + row_h, 0
                if y + ch > side:
                    ok = False
                    break
                self.cells[key]["x"], self.cells[key]["y"] = x + self.PAD, y + self.PAD
                x += cw
                row_h = max(row_h, ch)
            if ok:
                return side, side
        return None

    def uv(self, key, frac_uv):
        cell = self.cells[key]
        img = cell["scaled"]
        w, h = self.size
        # Yarım piksel içeri: kenardaki örnekleme komşuya taşmasın.
        u = (cell["x"] + 0.5 + frac_uv[:, 0] * (img.width - 1)) / w
        v = (cell["y"] + 0.5 + frac_uv[:, 1] * (img.height - 1)) / h
        return np.stack([u, v], axis=1)


def solid(rgb, alpha):
    from PIL import Image

    c = tuple(int(round(x * 255)) for x in np.clip(rgb, 0, 1)) + (int(round(np.clip(alpha, 0, 1) * 255)),)
    return Image.new("RGBA", (4, 4), c)


def material_image(mat, paint, glass, paint_rule=None):
    """Malzemenin atlasa girecek hali. Boyalı malzeme, araç rengiyle
    çarpılacağı için açık gri tonlara çevrilir."""
    from PIL import Image

    rgb, alpha = mat.average_srgb()
    if glass:
        return solid(rgb, float(np.clip(alpha if mat.blend else 0.45, 0.28, 0.7)))
    if mat.image is None:
        return solid((1, 1, 1) if paint else rgb, 1.0)
    arr = np.asarray(mat.image, dtype=np.float64) / 255.0
    lin = srgb_to_linear(arr[..., :3]) * mat.factor[:3]
    if paint:
        lum = (lin * np.array([0.2126, 0.7152, 0.0722])).sum(-1)
        sample = lum
        if paint_rule is not None:
            # Doku boya dışında renkler de içeriyorsa (iki renkli gövde),
            # referans parlaklık yalnızca boya rengindeki piksellerden alınır;
            # yoksa açık renkli kısım boyayı koyulaştırır.
            mask = color_mask(linear_to_srgb(lin), paint_rule)
            if mask.sum() > 32:
                sample = lum[mask]
        ref = np.percentile(sample, 85) if sample.size else 1.0
        if ref < 0.02:
            # Neredeyse siyah boya dokusu: normalize etmek yalnızca gürültüyü
            # büyütür; düz beyaz bırakıp rengi araca bırak.
            return solid((1, 1, 1), 1.0)
        g = linear_to_srgb(np.clip(lum / ref, 0, 1))
        out = np.dstack([g, g, g, np.ones_like(g)])
    else:
        out = np.dstack([linear_to_srgb(lin), arr[..., 3] * mat.factor[3]])
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


# ---------------------------------------------------------------------------
# Ana dönüşüm
# ---------------------------------------------------------------------------

def convert(car_id, cfg, body, preview=False):
    print("==", car_id, "-", cfg["title"], "/", cfg["author"])
    chunks = load_glb(download(cfg["uid"]))

    # 1) Yüzleri sınıflandır, atılacakları at.
    for ch in chunks:
        ch["kind"] = classify_chunk(ch, cfg)
    chunks = [c for c in chunks if (c["kind"] != KIND_DROP).any()]
    for ch in chunks:
        keep = ch["kind"] != KIND_DROP
        ch["f"], ch["kind"] = ch["f"][keep], ch["kind"][keep]

    # Modelin kendi sınır kutusuna göre (0..1) verilen kutulardaki yüzler
    # atılır: açık bırakılmış kapak gibi, ada göre seçilemeyen parçalar için.
    if cfg.get("drop_box"):
        all_v = np.concatenate([c["v"][np.unique(c["f"])] for c in chunks])
        lo, ext = all_v.min(0), np.maximum(all_v.max(0) - all_v.min(0), 1e-9)
        for ch in chunks:
            rel = (ch["v"][ch["f"]] - lo) / ext
            bad = np.zeros(len(ch["f"]), dtype=bool)
            for box in cfg["drop_box"]:
                b0, b1 = np.array(box[0]), np.array(box[1])
                bad |= ((rel >= b0) & (rel <= b1)).all(axis=(1, 2))
            ch["f"], ch["kind"] = ch["f"][~bad], ch["kind"][~bad]
        chunks = [c for c in chunks if len(c["f"])]

    # 2) Yön: uzun eksen Z, burun -Z, zemin y=0, ortalanmış.
    all_v = np.concatenate([c["v"][np.unique(c["f"])] for c in chunks])
    ext = all_v.max(0) - all_v.min(0)
    rot = np.eye(3)
    if ext[0] > ext[2]:
        rot = rotation_y(90.0)
    rot = rotation_y(cfg.get("yaw", 0.0)) @ rot
    if cfg.get("front", "-z") == "+z":
        rot = rotation_y(180.0) @ rot
    for ch in chunks:
        ch["v"] = ch["v"] @ rot.T
    all_v = np.concatenate([c["v"][np.unique(c["f"])] for c in chunks])
    lo, hi = all_v.min(0), all_v.max(0)
    target_len = body["length"] / 16.0
    scale = target_len / (hi[2] - lo[2])
    center = np.array([(lo[0] + hi[0]) / 2, lo[1], (lo[2] + hi[2]) / 2])
    for ch in chunks:
        ch["v"] = (ch["v"] - center) * scale

    # 3) Tekerlekler. İlk geçişte bulunan tekerleklere göre model yanlamasına
    # yeniden ortalanır (kapı, ayna gibi tek taraflı parçalar sınır kutusunu
    # kaydırabilir), sonra tekerlekler bir daha aranır.
    drop_shadow_planes(chunks, target_len)
    chunks = [c for c in chunks if len(c["f"])]

    def used_vertices():
        return np.concatenate([c["v"][np.unique(c["f"])] for c in chunks])

    wheels = detect_wheels(used_vertices(), target_len)
    shift = np.mean([w["cx"] for w in wheels])
    for ch in chunks:
        ch["v"] = ch["v"] - np.array([shift, 0.0, 0.0])
    wheels = detect_wheels(used_vertices(), target_len)
    ground = float(np.mean([w["cy"] - w["r"] for w in wheels]))
    for w in wheels:
        print("    tekerlek %s-%s r=%.3f z=%.3f x=%.3f..%.3f" % (
            "ön" if w["front"] else "arka", "sol" if w["left"] else "sağ", w["r"], w["cz"], w["x_in"], w["x_out"]))
    groups = {}
    for ch in chunks:
        kinds = ch["kind"].copy()
        wheel_of = np.full(len(kinds), -1)
        for wi, w in enumerate(wheels):
            mask = wheel_face_mask(ch["v"], ch["f"], w) & (kinds != KIND_GLASS) & (wheel_of < 0)
            wheel_of[mask] = wi
        kinds[wheel_of >= 0] = KIND_WHEEL
        # Işıklar: aracın ön yarısındakiler far, arka yarısındakiler stop.
        light = kinds == KIND_LIGHT
        if light.any():
            zc = ch["v"][ch["f"][light]].mean(1)[:, 2]
            kinds[np.nonzero(light)[0]] = np.where(zc < 0, KIND_LIGHT_FRONT, KIND_LIGHT_REAR)
        for kind in np.unique(kinds):
            for wi in np.unique(wheel_of[kinds == kind]):
                sel = (kinds == kind) & (wheel_of == wi)
                key = (int(kind), int(wi))
                groups.setdefault(key, []).append((ch, ch["f"][sel]))

    # Zemini tekerleklerin altına oturt.
    for ch in chunks:
        ch["v"] = ch["v"] - np.array([0.0, ground, 0.0])
    for w in wheels:
        w["cy"] -= ground

    # 4) Atlas hücreleri.
    atlas = Atlas()
    for (kind, wi), items in groups.items():
        for ch, f in items:
            mat = ch["mat"]
            ch.setdefault("cells", {})
            paint = kind == KIND_PAINT
            glass = kind == KIND_GLASS
            key = (id(mat), paint, glass)
            rule = cfg.get("paint_color")
            if rule is not None and rule.get("materials") and not matches(mat.name, rule["materials"]):
                rule = None
            atlas.add(key, material_image(mat, paint, glass, rule if paint else None))
            if mat.image is not None and not glass:
                avg_key = (id(mat), paint, "avg")
                rgb, _ = mat.average_srgb()
                atlas.add(avg_key, solid((1, 1, 1) if paint else rgb, 1.0))
    atlas_img = atlas.pack()

    # 5) Parça başına mesh, iki detay seviyesi.
    total = sum(len(f) for items in groups.values() for _, f in items)
    print("    toplam üçgen: %d, parça: %d" % (total, len(groups)))
    counts = {key: sum(len(f) for _, f in items) for key, items in groups.items()}
    ratios = [budget_ratios(counts, budget) for budget in LOD_BUDGETS]
    parts = []
    for (kind, wi), items in sorted(groups.items()):
        lods = []
        for li in range(len(LOD_BUDGETS)):
            lods.append(build_lod(items, kind, atlas, ratios[li][(kind, wi)], wheels[wi] if wi >= 0 else None))
        pivot = (0.0, 0.0, 0.0)
        flags = 0
        if wi >= 0:
            w = wheels[wi]
            pivot = (w["cx"], w["cy"], w["cz"])
            flags = (1 if w["front"] else 0) | (2 if w["left"] else 0)
        parts.append(dict(kind=kind, flags=flags, pivot=pivot, lods=lods))
    for li in range(len(LOD_BUDGETS)):
        print("    LOD%d: %d dörtgen" % (li, sum(len(p["lods"][li]["quads"]) for p in parts)))

    # 6) Spoiler yerleşimi için gövde yükseklik profili.
    body_v = []
    for (kind, wi), items in groups.items():
        if kind in (KIND_PAINT, KIND_PLAIN, KIND_GLASS):
            for ch, f in items:
                body_v.append(ch["v"][np.unique(f)])
    body_v = np.concatenate(body_v)
    half = target_len / 2
    bins = np.clip(((body_v[:, 2] + half) / target_len * PROFILE_BINS).astype(int), 0, PROFILE_BINS - 1)
    profile = np.zeros(PROFILE_BINS)
    np.maximum.at(profile, bins, body_v[:, 1])
    lo_all = body_v.min(0)
    hi_all = body_v.max(0)

    OUT_MESH.mkdir(parents=True, exist_ok=True)
    OUT_TEX.mkdir(parents=True, exist_ok=True)
    data = encode(parts, profile, hi_all - lo_all)
    (OUT_MESH / (car_id + ".rcm")).write_bytes(gzip.compress(data, 9))
    atlas_img.save(OUT_TEX / (car_id + ".png"), optimize=True)
    print("    yazıldı: %s.rcm (%d KB), atlas %dx%d (%d KB)" % (
        car_id, (OUT_MESH / (car_id + ".rcm")).stat().st_size // 1024, atlas_img.width, atlas_img.height,
        (OUT_TEX / (car_id + ".png")).stat().st_size // 1024))
    if preview:
        write_preview(car_id, parts, atlas_img)
    return dict(width=float(hi_all[0] - lo_all[0]), height=float(hi_all[1]), wheels=wheels)


# Bütçe paylaştırılırken parçaların ağırlığı: dış gövde göze en çok
# çarpan yüzeydir; lastik dişleri ve jant içi ise uzaktan seçilmez.
KIND_WEIGHT = {KIND_PAINT: 3.0, KIND_PLAIN: 1.0, KIND_GLASS: 1.5, KIND_LIGHT_FRONT: 1.5,
               KIND_LIGHT_REAR: 1.5, KIND_WHEEL: 0.5}


def budget_ratios(counts, budget):
    """Her parça için korunacak üçgen oranı: toplam bütçeye sığacak şekilde
    ağırlıkla orantılı (k * ağırlık, 1'i aşmadan), k ikiye bölmeyle bulunur."""
    def used(k):
        return sum(n * min(1.0, k * KIND_WEIGHT[key[0]]) for key, n in counts.items())
    if used(1e9) <= budget:
        return {key: 1.0 for key in counts}
    lo, hi = 0.0, 1.0
    while used(hi) < budget:
        hi *= 2
    for _ in range(50):
        mid = (lo + hi) / 2
        if used(mid) > budget:
            hi = mid
        else:
            lo = mid
    return {key: min(1.0, lo * KIND_WEIGHT[key[0]]) for key in counts}


def merge_by_material(items):
    """Aynı malzemeyi kullanan düğümleri tek mesh'te toplar; küçük düğümler
    tek başına sadeleştirilemez ama birlikte sadeleşir."""
    merged = {}
    for ch, f in items:
        if len(f) == 0:
            continue
        used, local = np.unique(f, return_inverse=True)
        entry = merged.setdefault(id(ch["mat"]), dict(mat=ch["mat"], v=[], uv=[], f=[], n=0))
        entry["v"].append(ch["v"][used])
        entry["uv"].append(ch["uv"][used])
        entry["f"].append(local.reshape(-1, 3) + entry["n"])
        entry["n"] += len(used)
    out = []
    for entry in merged.values():
        out.append((dict(mat=entry["mat"], v=np.concatenate(entry["v"]), uv=np.concatenate(entry["uv"])),
                    np.concatenate(entry["f"])))
    return out


def build_lod(items, kind, atlas, ratio, wheel):
    out_pos, out_nrm, out_uv, out_quads = [], [], [], []
    base = 0
    for ch, f in merge_by_material(items):
        if len(f) == 0:
            continue
        mat = ch["mat"]
        paint = kind == KIND_PAINT
        glass = kind == KIND_GLASS
        # Köşeler yalnızca konuma göre kaynatılır; UV köşe başına (wedge)
        # taşınır. Böylece UV dikişleri ve üçgen çorbası halindeki modeller
        # de sadeleşebilir, dikişleri MeshLab kendisi korur.
        corner_uv = ch["uv"][f]
        # Dokusu olup UV'si olmayan (hepsi aynı noktada) malzeme fiilen düz
        # renktir; dokulu sadeleştirme onda hiçbir şey yapamıyor.
        textured = mat.image is not None and not glass and np.ptp(corner_uv.reshape(-1, 2), axis=0).max() > 1e-6
        v, wf, _, keep = weld(ch["v"], f, np.zeros_like(ch["uv"]))
        n_in = len(wf)
        v, wf, cuv = simplify(v, wf, corner_uv[keep], ratio, textured)
        if VERBOSE:
            print("      %-40s tür=%d %6d -> %6d üçgen (oran %.3f)" % (mat.name[-40:], kind, n_in, len(wf), ratio))
        if len(wf) == 0:
            continue
        cn = corner_normals(v, wf)
        # Köşe başına atlas UV'si: yüz tek bir doku karesine sığıyorsa
        # kendi karesine, sığmıyorsa (tekrarlanan doku) ortalama renge.
        tile = np.floor(cuv.min(1) + 1e-6)
        frac = cuv - tile[:, None, :]
        fits = (frac.max(axis=(1, 2)) <= 1.0 + 1e-3)
        frac = np.clip(frac, 0.0, 1.0)
        key = (id(mat), paint, glass)
        auv = atlas.uv(key, frac.reshape(-1, 2)).reshape(-1, 3, 2)
        if mat.image is not None and not glass and not fits.all():
            avg = atlas.uv((id(mat), paint, "avg"), np.full((int((~fits).sum()) * 3, 2), 0.5)).reshape(-1, 3, 2)
            auv[~fits] = avg
        if mat.image is None or glass:
            auv[:] = atlas.uv(key, np.full((len(wf) * 3, 2), 0.5)).reshape(-1, 3, 2)
        # Köşeleri (konum, normal, uv) üçlüsüne göre tekilleştir.
        pos = v[wf].reshape(-1, 3)
        nrm = cn.reshape(-1, 3)
        uvs = auv.reshape(-1, 2)
        key_arr = np.concatenate([np.repeat(wf.reshape(-1, 1), 1, axis=1).astype(np.float64),
                                  np.round(nrm * 64), np.round(uvs * 8192)], axis=1)
        uniq, first, inverse = np.unique(key_arr, axis=0, return_index=True, return_inverse=True)
        tri = inverse.reshape(-1, 3)
        p_u, n_u, uv_u = pos[first], nrm[first], uvs[first]
        quads = pair_quads(tri, p_u)
        if wheel is not None:
            p_u = p_u - np.array([wheel["cx"], wheel["cy"], wheel["cz"]])
        out_pos.append(p_u)
        out_nrm.append(n_u)
        out_uv.append(uv_u)
        out_quads.append(quads + base)
        base += len(p_u)
    if not out_pos:
        return dict(pos=np.zeros((0, 3)), nrm=np.zeros((0, 3)), uv=np.zeros((0, 2)), quads=np.zeros((0, 4), int))
    return dict(pos=np.concatenate(out_pos), nrm=np.concatenate(out_nrm), uv=np.concatenate(out_uv),
                quads=np.concatenate(out_quads))


def encode(parts, profile, size):
    buf = io.BytesIO()
    w = buf.write
    w(FORMAT_MAGIC)
    w(struct.pack("<H", FORMAT_VERSION))
    w(struct.pack("<3f", *size))
    w(struct.pack("<B", len(profile)))
    w(struct.pack("<%df" % len(profile), *profile))
    w(struct.pack("<H", len(parts)))
    for part in parts:
        w(struct.pack("<BB3f", part["kind"], part["flags"], *part["pivot"]))
        w(struct.pack("<B", len(part["lods"])))
        for lod in part["lods"]:
            n = len(lod["pos"])
            w(struct.pack("<I", n))
            w(lod["pos"].astype("<f4").tobytes())
            nrm = lod["nrm"] / np.maximum(np.linalg.norm(lod["nrm"], axis=1, keepdims=True), 1e-9)
            w(np.clip(np.round(nrm * 127), -127, 127).astype("<i1").tobytes())
            w(np.clip(np.round(lod["uv"] * 65535), 0, 65535).astype("<u2").tobytes())
            q = len(lod["quads"])
            w(struct.pack("<I", q))
            w(lod["quads"].astype("<u4").tobytes())
    return buf.getvalue()


def write_preview(car_id, parts, atlas_img):
    """Dönüştürülmüş modeli gözle kontrol etmek için GLB (yalnızca geliştirme)."""
    import trimesh

    scene = trimesh.Scene()
    tints = {KIND_PAINT: (255, 60, 200, 255), KIND_WHEEL: None}
    for i, part in enumerate(parts):
        lod = part["lods"][0]
        if len(lod["quads"]) == 0:
            continue
        q = lod["quads"]
        tris = np.concatenate([q[:, [0, 1, 2]], q[:, [0, 2, 3]]])
        tris = tris[(tris[:, 0] != tris[:, 1]) & (tris[:, 1] != tris[:, 2]) & (tris[:, 0] != tris[:, 2])]
        pos = lod["pos"] + np.array(part["pivot"])
        material = trimesh.visual.material.PBRMaterial(baseColorTexture=atlas_img, doubleSided=True,
                                                       baseColorFactor=tints.get(part["kind"]) or (255, 255, 255, 255),
                                                       alphaMode="BLEND" if part["kind"] == KIND_GLASS else "OPAQUE")
        visual = trimesh.visual.TextureVisuals(uv=np.column_stack([lod["uv"][:, 0], 1 - lod["uv"][:, 1]]),
                                               material=material)
        scene.add_geometry(trimesh.Trimesh(pos, tris, visual=visual, process=False), node_name="part%d" % i)
    out = CACHE / "preview"
    out.mkdir(parents=True, exist_ok=True)
    scene.export(str(out / (car_id + ".glb")))


def write_credits():
    """CC-BY 4.0 atıf zorunluluğu: eser adı, yazar, kaynak, lisans ve
    yapılan değişiklikler. Hem depo kökünde hem jar'ın içinde durur."""
    lines = [
        "# Atıflar",
        "",
        "## 3B araç modelleri",
        "",
        "Araçların yüksek poligonlu gövdeleri, aşağıdaki sanatçıların Sketchfab'da",
        "[Creative Commons Atıf 4.0](%s) lisansıyla yayımladığı modellerden" % hd_models.LICENSE_URL,
        "dönüştürülmüştür. Modeller, lisans bilgisiyle birlikte",
        "[Objaverse](%s) arşivinden indirildi." % hd_models.SOURCE,
        "",
        "Yapılan değişiklikler (hepsi `tools/import_hd_models.py` ile): ölçek ve yön",
        "araç boyuna göre ayarlandı; çokgen sayısı azaltıldı (yakın görünüm en fazla",
        "~%d, uzak görünüm ~%d üçgen); dokular tek bir atlasa toplandı; boya" % LOD_BUDGETS,
        "malzemesi oyunda yeniden boyanabilsin diye griye çevrildi; tekerlekler dönebilsin",
        "diye gövdeden ayrıldı; sahne parçaları (zemin gölgesi vb.) çıkarıldı.",
        "",
        "Marka ve model adları ilgili üreticilerin ticari markalarıdır; modellerin",
        "yazarları ve bu mod o üreticilerle bağlantılı değildir.",
        "",
        "| Araç | Model | Yazar | Lisans |",
        "|---|---|---|---|",
    ]
    for car_id, cfg in hd_models.HD_MODELS.items():
        lines.append("| `%s` | [%s](%s) | %s | CC BY 4.0 |" % (
            car_id, cfg["title"].replace("|", "/"), hd_models.sketchfab_url(cfg["uid"]), cfg["author"]))
    import import_engine_sounds

    lines += import_engine_sounds.credits_section()
    text = "\n".join(lines) + "\n"
    (ROOT / "CREDITS.md").write_text(text, encoding="utf-8")
    (OUT_MESH / "CREDITS.md").write_text(text, encoding="utf-8")


def main(argv):
    preview = "--preview" in argv
    names = [a for a in argv if not a.startswith("--")] or list(hd_models.HD_MODELS)
    bodies = car_bodies()
    report = {}
    for name in names:
        cfg = hd_models.HD_MODELS[name]
        report[name] = convert(name, cfg, bodies[name], preview)
    write_credits()
    return report


if __name__ == "__main__":
    main(sys.argv[1:])
