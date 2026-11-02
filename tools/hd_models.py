"""Yüksek poligonlu araç modellerinin kaynak tablosu.

Her araç için internetten indirilen açık lisanslı (CC-BY 4.0) bir 3B modelin
nereden geldiği ve dönüştürücünün (`import_hd_models.py`) onu nasıl
yorumlayacağı burada yazar. Modeller Sketchfab'da yayımlanmış, Objaverse
veri setinde (Allen AI, huggingface.co/datasets/allenai/objaverse) lisans
bilgisiyle birlikte arşivlenmiş GLB dosyalarıdır; dönüştürücü onları oradan
indirir.

Alanlar:
  uid       : Sketchfab / Objaverse model kimliği
  title     : modelin Sketchfab'daki adı
  author    : Sketchfab kullanıcı adı (atıf için)
  front     : dönüştürücü modeli uzun ekseni Z olacak şekilde çevirdikten sonra
              burnun baktığı yön, "+z" ya da "-z"
  paint     : boyanan (sprey boyayla rengi değişen) malzemeler
  glass     : yarı saydam çizilen cam malzemeleri
  lights    : far/stop malzemeleri; öndekiler far, arkadakiler stop olarak
              ayrılır (konuma bakılarak)
  drop      : atılacak düğüm ya da malzeme adları (zemin gölgesi, sahne süsü...)
  drop_box  : modelin kendi sınır kutusuna göre (0..1) verilen kutulardaki
              yüzler atılır (adı olmayan parçalar için)
  paint_color: dokuya gömülü boyayı renk tonuna göre ayırır; dict(color=(r,g,b),
              tol=..., materials=[...])
  light_nodes / paint_nodes: malzeme yerine düğüm adına göre seçim
  yaw       : uzun eksen otomatik bulunamazsa elle dönüş (derece)

Ad listeleri büyük/küçük harf duyarsız alt dizgi eşleşmesidir: "glass",
"GlassHeadlight" ile de eşleşir. Bir malzeme hem cam hem far listesine
uyuyorsa far sayılır.
"""

LICENSE = "CC-BY-4.0"
LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/"
SOURCE = "https://huggingface.co/datasets/allenai/objaverse"

HD_MODELS = {
    "golf_gti": dict(
        uid="877b82ed48954e08a234dae968b70704",
        title="Volkswagen Golf Gti 16V", author="LePointBAT",
        front="-z",
        paint=["Paint"], glass=["Windows"],
        lights=["lights", "Y_lights"],
        drop=[]),
    "corolla": dict(
        uid="fe02fba6302e450ea8424591493341ea",
        title="Toyota Corolla AE86 Trueno", author="Lexyc16",
        front="+z",
        paint=["=Material.001"], glass=[], lights=[], drop=[]),
    "bmw_m3": dict(
        uid="8fa21fe97a6042a2a09e0b09fd546b91",
        title="BMW M3 Coupe (E30) 1986", author="Lexyc16",
        front="+z",
        paint=["=Material.001"], glass=[], lights=["=Material.014", "=Material.011", "=Material", "=Material.022"], drop=[]),
    "mustang_gt": dict(
        uid="4223fcc87baa4ac58be2910a5367d7d0",
        title="'67 Mustang (High poly)", author="nickbroad",
        front="+z",
        paint=["Paint"], glass=["Glass"], lights=["Headlight", "TailLight", "Emiss"],
        drop=[]),
    "gtr": dict(
        uid="8810e677eca543a6b840cf67dd064aa4",
        title="Nissan Skyline R32 GT-R", author="Lexyc16",
        front="+z",
        paint=["car_paint"], glass=["glass"], lights=["light"], drop=[]),
    "porsche_911": dict(
        uid="8568d9d14a994b9cae59499f0dbed21e",
        title="FREE 1975 Porsche 911 (930) Turbo", author="karolmiklas",
        front="+z",
        paint=["paint"], glass=["glass"], lights=["930_lights"], drop=["=material_0", "=coat"]),
    "wrangler": dict(
        uid="178f22467bae4c729bdcc15dbc7e445d",
        title="Jeep Rubicon", author="Armawiz",
        front="+z",
        paint=["=Body"], glass=["windows"], lights=["Lights"], drop=[]),
    "hilux": dict(
        uid="0234ebed01ee4b87affed0e58673247e",
        title="Toyota Hilux 2007", author="David_Holiday",
        front="+z",
        paint=["carpaint"], glass=[], lights=[], drop=[]),
    "defender": dict(
        uid="26f042fcf1594890a529bfb41f0fae0d",
        title="2021 Land Rover Defender 110", author="David_Holiday",
        front="+z",
        paint=["2020body_1"], glass=["glass"], lights=["red"], drop=[]),
    "beetle": dict(
        uid="cb6e82e73386448fb3b4e2dd25255a5e",
        title="Beetle/Fusca (Version 1)", author="soujagah",
        front="-z",
        paint=["=material"], glass=[], lights=[], drop=[]),
    "c10": dict(
        uid="14116da06a65420db32b585466061d31",
        title="1983 Chevrolet C10", author="korsun",
        front="-z",
        paint=["lambert_1011", "lambert_1012"], glass=[], lights=[], drop=[], light_nodes=["lights_down", "lights_up", "back_lights", "Lights_lambert"]),
    "transporter": dict(
        uid="d34fd744db1c4f84ad40c854ddbff9e9",
        title="Volkswagen Kombi Micro Bus", author="winterborn",
        front="+z",
        # İki renkli boya tek dokuda: yalnızca mavi alt gövde boyanır,
        # beyaz üst kısım klasik iki renkli görünüm için kalır.
        paint=[], glass=["glass"], lights=["headlight"], drop=[],
        paint_color=dict(materials=["Car_Paint_MAT"], color=(50, 120, 200), tol=0.3),
        # Modelde motor kapağı açık; arkadan sarkan kapak atılır.
        drop_box=[((0.25, 0.43, 0.0), (0.75, 0.47, 0.085))]),
}


def sketchfab_url(uid):
    return "https://sketchfab.com/3d-models/" + uid
