# RealCars — Minecraft 26.3 Araba Modu

Gerçek hayattaki arabaları Minecraft'a getiren bir Fabric modu: parçalardan araba
craftlarsın, motoru çalıştırıp sürersin, km/saat göstergesinden hızını görürsün,
tekerlekler döner, toprakta ve kumda arkandan toz kalkar, garajda rengini,
tekerleğini ve rüzgarlığını değiştirirsin.

| | |
|---|---|
| **Minecraft** | 26.3 (Java Edition) |
| **Mod yükleyici** | Fabric Loader 0.20.1+ |
| **Gerekli** | Fabric API 0.165.0+26.3 |
| **Java** | 25 (Minecraft 26.x zorunlu kılıyor) |
| **3B modeller** | Gerçek araç modelleri, CC BY 4.0 — bkz. [CREDITS.md](CREDITS.md) |

---

## Kurulum

1. [Fabric Loader](https://fabricmc.net/use/) 0.20.1 veya üstünü Minecraft 26.3 için kur.
2. [Fabric API](https://modrinth.com/mod/fabric-api) sürüm `0.165.0+26.3`'yi indir.
3. Bu modun `realcars-1.1.0.jar` dosyasını ve Fabric API'yi `.minecraft/mods`
   klasörüne at.
4. Oyunu Fabric profiliyle başlat.

Kaynaktan derlemek için: `JAVA_HOME=<jdk25> ./gradlew build` → `build/libs/realcars-1.1.0.jar`

---

## Nasıl oynanır

### 1. Parçaları yap

Her şey **çelik levhayla** başlar. Sıradan bir çalışma tezgahı yeterlidir; garaj
havası istersen **Montaj Tezgahı** da aynı işi görür.

| Parça | Tarif |
|---|---|
| Çelik Levha ×4 | 2×2 demir külçe |
| Kauçuk ×3 | 2 balçık topu + 1 kömür |
| Cam Panel ×4 | 2×2 cam |
| Tekerlek ×2 | Halka şeklinde kauçuk + ortada göbek (göbek tipi tekerleği belirler) |
| Motor | Pistonlar + çelik + demir bloğu + kızılkaya bloğu |
| Şanzıman | Demir külçesi + çelik + kızılkaya (otomatikte karşılaştırıcı) |
| Şasi | 8 çelik levha + ortada sınıfa özgü blok |
| Araç Koltuğu | Deri + yün + çelik |
| Ön Cam | 6 cam panel + 3 çelik |
| Araba Yapma Masası | 4 çelik levha + montaj tezgahı + 3 demir bloğu |

Tam listeyi oyun içindeki **tarif kitabından** görebilirsin. Modun bütün
tarifleri, dünyaya girdiğin anda kitabına açılmış olarak gelir: elinde tek bir
parça olmasa bile hepsi listede durur ve yapılışını görebilirsin.

Kitabı açmadan bakmak istersen **`/cars recipes`** komutu var:

| Komut | Ne yapar |
|---|---|
| `/cars recipes` | Modun ürettiği her şeyin listesi; bir ada tıklayınca yapılışı yazılır |
| `/cars recipes asphalt` | Tek bir eşyanın tarifi, 3×3 ızgara olarak |
| `/recipe cars ...` | Aynısı, vanilla `/recipe` komutunun altından (yetkili oyuncu için) |

```
Asfalt x8
  [A][A][A]
  [A][B][A]
  [A][A][A]
  A = Çakıl
  B = Kömür Bloğu
```

### 2. Arabayı craftla

En kolay yol **Araba Yapma Masası**: her parçanın kendi gözü var — şasi, motor,
şanzıman, tekerlek (dört adet), koltuk ve ön cam. Hangi gözün ne olduğu üstüne
gelince yazılır, doğru bileşimi koyunca çıkacak araba sağdaki gözde belirir.

Sağ üstte **on iki arabanın katalogu** durur. Bir arabanın üstüne gelince adı ve
istediği parçalar listelenir; tıklarsan parçalar envanterinden alınıp gözlere
dizilir. Eksik parça varsa hangisi olduğu ekranın altında yazar, yani neyi
craftlaman gerektiğini görürsün.

Hangi arabanın çıkacağına masa kendi karar vermez; parçaları aracın gerçek
kalıbına dizip tariflere sorar, yani tariflerle her zaman tutarlıdır.

İstersen sıradan bir çalışma tezgahında (ya da **Montaj Tezgahı**'nda) da
yapabilirsin. Bütün arabalar aynı 3×3 kalıbı kullanır:

```
tekerlek | ön cam  | tekerlek
motor    | şasi    | şanzıman
tekerlek | koltuk  | tekerlek
```

Hangi arabanın çıkacağını **şasi + motor + şanzıman + tekerlek** bileşimi
belirler. Örneğin spor şasi + V8 + manuel + spor tekerlek = **Ford Mustang GT**.
Şanzıman tipi hangi arabanın çıkacağını ve vites sayısını belirler; sürerken
hepsi kendi kendine vites değiştirir.

### 3. Sür

Arabayı yere koy (bir bloğun üstüne sağ tıkla), üzerine sağ tıklayarak bin.

| Tuş | İşlev |
|---|---|
| `G` | **Motoru çalıştır / durdur — bunsuz araba hareket etmez** |
| `W` | İleri |
| `S` | Geri (ileri giderken basarsan önce fren yapar) |
| `A` / `D` | Direksiyon |
| `Boşluk` | El freni |
| `H` | Korna |
| `Shift` | Araçtan in |

Tuşlar oyun ayarlarından **RealCars** başlığı altında değiştirilebilir. Araca
bindiğinde bu liste sohbete de yazılır — hem de senin gerçekten atadığın
tuşlarla.

**Vites yok.** Bütün araçlarda şanzıman kendi kendine çalışır: `W` ileri, `S`
geri, o kadar. Göstergedeki vites numarası sadece bilgi içindir.

**El freni neden boşlukta?** Eğilme tuşu Minecraft'ta araçtan indirir; el
freni oraya bağlansaydı fren yapmakla inmek aynı tuş olurdu.

Araca binince kamera kendiliğinden üçüncü şahsa geçer ve araçtayken birinci
şahsa düşmez — hem aracı hem yolu görürsün. İndiğinde eski görünümüne dönersin.
Binen oyuncunun modeli çizilmez, yalnızca isim etiketi kalır: yani arabada kim
olduğu görünür ama içine tıkışmış bir oyuncu görünmez.

**İki kişi binebilir** (bazı araçlarda dört). Aracı ilk binen sürer; sonradan
binenin tuşları direksiyona karışmaz.

### 4. Modifiye et

İki yol var:

- **İngiliz Anahtarı** elindeyken arabaya sağ tıkla, ya da
- **Araç Lifti** bloğunu arabanın yanına koyup lifte sağ tıkla.

Lift ve **Yakıt Pompası** yalnızca yanlarında araç varken iş görür; araç yoksa
ekranın altında bunu söylerler.

Açılan ekranda dört sekme var: **Renk**, **Tekerlek**, **Rüzgarlık**,
**Motor ve Şanzıman**. Solda araba dönerek gösterilir ve yaptığın her değişiklik
anında yansır.

- Renk değişimi bir **sprey boya** harcar (16 renk, boyalardan yapılır).
  Aceleyse spreyi doğrudan arabaya sağ tıklayarak da sürebilirsin.
- Tekerlek değişimi envanterinde **4 adet** o tekerlekten ister; söktüğün 4
  tekerleği geri alırsın.
- Motor, şanzıman ve rüzgarlık birer adet ister ve eskisini geri verir.

Rüzgarlık sadece süs değil: bastırma kuvveti verdiği için yüksek hızda
direksiyon daha iyi tutar. Tekerlek seçimi de gerçekten önemli — spor lastik
asfaltta, arazi lastiği toprakta kazandırır.

### 5. Yakıt

Depo boşalırsa motor durur. İki doldurma yolu var:

- **Yakıt Bidonu** ile arabaya sağ tıkla (+25 L, bidon tükenir).
- **Yakıt Pompası** bloğunun yanına park et ve pompaya sağ tıkla — envanterindeki
  her kömür 20 L doldurur.

---

## Araçlar

| Araç | Sınıf | Azami hız | 0-100 | Motor | Şanzıman | Tekerlek |
|---|---|---|---|---|---|---|
| Volkswagen Golf GTI | Hatchback | 250 km/s | 6.2 sn | I4 | Manuel | Spor |
| Toyota Corolla | Sedan | 190 km/s | 10.4 sn | I4 | Otomatik | Sokak |
| BMW M3 | Sedan | 290 km/s | 4.1 sn | I4 | Manuel | Spor |
| Ford Mustang GT | Spor | 250 km/s | 4.3 sn | V8 | Manuel | Spor |
| Nissan GT-R | Spor | 315 km/s | 2.9 sn | I6 | Spor DCT | Spor |
| Porsche 911 | Spor | 300 km/s | 3.4 sn | Boksör 6 | Spor DCT | Spor |
| Jeep Wrangler | Arazi | 180 km/s | 7.6 sn | V6 | Otomatik | Arazi |
| Toyota Hilux | Kamyonet | 175 km/s | 11.0 sn | I4 | Manuel | Arazi |
| Land Rover Defender | SUV | 190 km/s | 6.6 sn | I6 | Otomatik | Arazi |
| Volkswagen Beetle | Klasik | 130 km/s | 17.5 sn | Boksör 4 | Manuel | Sokak |
| Chevrolet C10 | Klasik kamyonet | 160 km/s | 9.0 sn | V8 | Manuel | Sokak |
| VW Transporter T1 | Minibüs | 105 km/s | 22.0 sn | Boksör 4 | Manuel | Sokak |

---

## Yol yapımı

**Asfalt**, **Asfalt Levha** ve iki renk **Yol Çizgisi** blokları var. Çizgiler
onları koyarken baktığın yöne göre uzanır.

Asfalt en yüksek tutuşu verir; taş ve beton onu izler; toprak, kum ve çakılda
tutuş düşer, buz ve karda daha da düşer. Yani gerçekten yol yapmak işe yarar:
aynı arabayla asfaltta çıkardığın hızı çimende çıkaramazsın.

---

## Araç gövdeleri nasıl çiziliyor

Her araç, internetten indirilmiş **açık lisanslı, gerçekçi, yüksek poligonlu bir
3B modelle** çizilir: Golf GTI 16V, AE86 Corolla, E30 M3, '67 Mustang, R32
Skyline GT-R, 930 Turbo, Wrangler Rubicon, Hilux, yeni Defender 110, Beetle,
C10 ve T1 Kombi. Modeller Sketchfab'da CC BY 4.0 lisansıyla yayımlanmış
çalışmalardır; yazarları ve bağlantıları [CREDITS.md](CREDITS.md) dosyasında
(ve jar'ın içinde `assets/realcars/hd/CREDITS.md`) yazar.

Modeller oyuna doğrudan GLB olarak girmez; `tools/import_hd_models.py` onları
modun kendi biçimine çevirir:

- **Ölçek ve yön.** Model, aracın gerçek boyuna ölçeklenir (1 blok = 1 metre),
  burnu öne bakacak şekilde çevrilir ve tekerleklerinin altı zemine oturtulur.
- **Tekerlekler gövdeden ayrılır.** İndirilen modellerde tekerlek ayrı bir parça
  olarak gelmez; dönüştürücü her köşede yere değen lastiği bulur, çember
  yarıçapını lastiğin çizdiği tam daireye göre seçer ve o silindirin içindeki
  üçgenleri ayrı parçaya alır. Bu sayede tekerlekler yolda yuvarlanır, ön
  tekerlekler direksiyonla döner. Bulunan ölçüler gerçeğe çok yakındır (ör.
  Hilux aks arası 3.08 m, gerçeği 3.085 m).
- **Boya.** Gövde boyası ayrı bir parçaya alınıp griye çevrilir; oyunda sprey
  boyayla seçtiğin renk onunla çarpılır, yani gerçek modelin bütün kabartmaları
  ve çizgileri üstünde istediğin renk görünür. Dokuya gömülü iki renkli boyada
  (T1 Kombi) yalnızca boya rengindeki bölge boyanır.
- **Cam, far, stop.** Cam yarı saydam çizilir; farlar motor çalışınca, stoplar
  fren yapınca ışık yayan ayrı bir geçişte parlar.
- **İki detay seviyesi.** Yakın görünüm 15–60 bin dörtgendir; 40 bloktan ötede
  çok daha hafif bir sürüm çizilir. Sadeleştirme MeshLab'in quadric yöntemiyle,
  yüz normalini ters çevirecek adımlar reddedilerek yapılır (yoksa ince kapı
  panellerinde boya kaybolup içi görünüyordu).
- **Tek doku.** Bir modelin bütün dokuları ve düz renkleri tek bir atlas
  sayfasında toplanır, böylece araç birkaç çizim çağrısıyla çizilir.

Araç sürerken **gövde ile tekerlekler ayrı hareket ediyor**: virajda gövde yana
yatıyor, frende burun dalıyor, tekerlekler yerde kalıyor. Rüzgarlıklar
modifiye parçası olduğu için hâlâ modun kendi parçalarıdır; gerçek modelin
bagaj kapağı yüksekliğine oturtulurlar.

### Yedek: parametrik gövde

`.rcm` dosyası bir kaynak paketinden çıkarılırsa ya da okunamazsa araç, modun
ilk sürümlerindeki parametrik gövdeyle çizilir. O gövde `CarModel.java`
içindeki **yan profilden** (burun yüksekliği, kaputun bittiği yer, cam altı
hattı, tavanın iki ucu, kuyruk) ince eğik panellerle kurulur; tekerlekler 12
kenarlı pikselli yuvarlaklardır. Yani bir kaynak paketi araç modellerini
kendi modelleriyle değiştirebilir ya da tamamen kapatabilir.

---

## Bilmen gereken birkaç şey

- **Hız gerçek ölçekte.** 1 blok = 1 metre kabul edilir, dolayısıyla göstergedeki
  sayı oyundaki gerçek hızdır. Bunun bir sonucu var: 250 km/s saniyede ~69 blok
  demek ve bu, sunucunun chunk yükleme hızını zorlayabilir. Uzun yolculuklarda
  görüş mesafesini artırmak ya da yolu önceden yüklenmiş tutmak yardımcı olur.
- **Kapı sesi.** Araca binip inerken kapı sesi çalar; sunucudan çalındığı için
  yanından geçen birinin bindiğini de duyarsın.
- **Motor sesleri gerçek kayıtlardan.** Sesler açık lisanslı (CC0 / CC BY) gerçek
  motor kayıtlarından kesildi; mümkün olan her araçta kendi motorundan: Corolla'da
  AE86'nın 4A-GE'si, M3'te bir BMW M3, Mustang'de bir Mustang V8, 911'de bir
  Porsche, Beetle'da 1961 bir Beetle, T1'de bir VW Type 2, C10'da bir Chevrolet 350
  V8, Wrangler'da bir Jeep. Diğerleri en yakın kayıttan (dizeller için turbo dizel
  kamyonet/van, Golf için oyun için kaydedilmiş bir benzinli otomobil seti). Her
  kayıttan rölanti, orta ve yüksek devirde üç döngü çıkarıldı; oyunda devre göre
  üçü arasında yumuşakça geçilir ve her biri kendi kayıt devrine göre perdelenir.
  Yani motor tek bir sesi incelterek değil, her devirde kendine yakın gerçek bir
  kaydın tınısıyla yükselir. Modifiyede başka bir motor takarsan ses de o motora
  geçer: Golf'e V8 takınca Mustang gibi gürler. Kaynaklar ve yazarlar
  [CREDITS.md](CREDITS.md) içinde.
- **Yokuş çıkar, hendeğe düşmez.** Araç bir bloklık basamakları ve merdivenleri
  tırmanır. Çarpışma kutusu aracın uzunluğunu kapsamadığı için dar bir boşluğa
  sığar; bu yüzden zemin desteği kutuya değil gerçek aks konumlarına bakılarak
  belirlenir — iki aksdan biri karadaysa araç boşluğa köprü kurar, düşmez.
- **Gösterge yalan söylemez.** Hız, tick içinde gerçekten alınan yola göre
  düzeltilir. Duvara dayanmış araç sıfır gösterir, çamurda patinaj yaparken de
  gerçekte ne kadar ilerliyorsan onu.
- **Çarpışma kutusu aracın genişliği kadardır**, uzunluğu kadar değil.
  Minecraft'ta çarpışma kutusu yatayda karedir; 5 metrelik bir araba için 3
  bloklık bir kare demek yolun kenarına sürekli sürtmek demekti. Karşılığında
  araç uzunlamasına bloklara biraz girer.
- **Fren gerçekten zemine bağlı.** `S` frendir, `Shift` el freni. Fren gücü
  zemin tutuşuyla çarpılır, yani asfaltta kısa, toprakta uzun mesafede
  durursun. Sert frende stop lambaları yanar, lastikler gıcırdar ve gövdenin
  burnu dalar.
- **Tekerlek yolda kaymadan yuvarlanır.** Dönme açısı kat edilen yoldan
  hesaplanır, yani lastik yerde kaymaz — görsel test bunu her koşuda sayıyla
  doğruluyor. Patinaj anında ise bilerek yoldan hızlı döner.

---

## Marka isimleri hakkında

Araçlar gerçek marka ve model isimleriyle anılıyor. Bu isimler ilgili
üreticilerin tescilli markalarıdır ve bu mod onlarla bağlantılı değildir, onlar
tarafından onaylanmamıştır. Marka logolarının kopyası kullanılmamıştır.

Kişisel ve özel sunucu kullanımı için sorun yoktur. Modu Modrinth/CurseForge gibi
halka açık platformlarda **yayınlamayı düşünüyorsan** isimleri değiştirmen
önerilir — `tools/spec.py` içindeki `CARS` tablosunda `tr` / `en` alanlarını
düzenleyip `python3 tools/gen_data.py` çalıştırman yeterli.

---

## Geliştirme

Dokular, sesler ve veri JSON'ları elle yazılmaz; `tools/` altındaki üreticiler
tarafından `tools/spec.py`'deki tek tabloya bakılarak oluşturulur.

```bash
pip install numpy Pillow soundfile

python3 tools/gen_textures.py     # araç atlası, item ikonları, blok dokuları, HUD kadranı
python3 tools/gen_sounds.py       # marş, korna, fren vb. (FFT sentezi) -> .ogg
python3 tools/import_engine_sounds.py  # gerçek kayıtlardan motor sesleri (bkz. aşağı)
python3 tools/gen_data.py         # tarifler, modeller, blockstate, dil dosyaları
python3 tools/validate_assets.py  # kırık referans / tarif çakışması / eksik çeviri kontrolü
```

**Yüksek poligonlu modeller** `tools/hd_models.py` tablosundan üretilir:

```bash
pip install trimesh pymeshlab scipy numpy Pillow
# pymeshlab'in sadeleştirme eklentisi libOpenGL ister (Debian/Ubuntu):
sudo apt-get install libopengl0 libglu1-mesa

python3 tools/import_hd_models.py              # hepsi
python3 tools/import_hd_models.py beetle       # tek araç
python3 tools/import_hd_models.py --preview    # tools/.hd_cache/preview altına kontrol GLB'leri
```

**Motor sesleri** `tools/sound_sources.py` tablosundaki Freesound kayıtlarından
üretilir; kayıtlar Freesound'un Hugging Face'teki lisans bilgili aynasından
indirilir (`pip install numpy scipy soundfile pyarrow huggingface_hub`):

```bash
python3 tools/import_engine_sounds.py          # hepsi
python3 tools/import_engine_sounds.py beetle   # tek ses
```

Tabloda her ses için rölanti / orta / yüksek devir katmanlarının hangi kayıttan
ve hangi saniye aralığından alınacağı yazar (spektrograma bakılarak seçildi).
Araç penceredeki harmonik çizgiyi izleyerek perdeyi sabitler, dikişsiz döngüye
çevirir, katmanların devir oranını spektrum hizalamasıyla ölçüp
`assets/realcars/engine_voices.json`'a yazar ve CREDITS.md'yi günceller.

Dönüştürücü modelleri ilk çalıştırmada Objaverse arşivinden indirir ve
`tools/.hd_cache` altında saklar. Tabloda her model için kaynak kimliği, yazar,
burnun yönü ve hangi malzemenin boya / cam / far olduğu yazar; başka bir modelle
değiştirmek için oradaki satırı düzenlemek yeterlidir. Yeni bir model eklerken
lisansının CC0 ya da CC BY olmasına dikkat et ve `--preview` çıktısına bak:
önizlemede boyanan yüzeyler pembe görünür.

**Yeni araç eklemek** iki dosyaya birer kayıt yazmaktır:

1. `tools/spec.py` → `CARS` tablosuna bir satır
2. `src/main/java/.../entity/CarModel.java` → aynı isimle bir enum sabiti (ölçüler
   ve fizik değerleriyle)

Sonra üreticileri çalıştır; doku, ikon, tarif, dil anahtarı ve 3B gövde
otomatik üretilir. `validate_assets.py` iki dosyanın uyumunu kontrol eder.
Gerçek bir 3B model de istiyorsan `tools/hd_models.py`'ye üçüncü bir kayıt ekle
ve `import_hd_models.py`'yi çalıştır; yoksa araç parametrik gövdeyle çizilir.

### Testler

```bash
JAVA_HOME=<jdk25> ./gradlew build     # derleme
python3 tools/validate_assets.py      # kaynak bütünlüğü
JAVA_HOME=<jdk25> tools/smoke_test.sh # başsız sunucuda 12 aracı spawn edip günlüğü tarar

# Oyunu gerçekten açan görsel test: dünya kurar, on iki aracın her birini tek
# tek yandan ve 3/4 açıdan fotoğraflar, sonra bir tanesine binip hızlanmayı,
# virajda yatmayı, frende burun dalmasını ve stop lambalarını sayılarla
# doğrular. Ayrıca her aracın gerçekten yüksek poligonlu gövdeyle çizildiğini,
# yeni giren oyuncunun tarif kitabında modun bütün tariflerinin açık olduğunu
# ve /cars recipes komutunun çalıştığını sınar.
# Görüntüler build/run/clientGametest/screenshots altına düşer.
JAVA_HOME=<jdk25> ./gradlew runClientGametest
# Başsız bir makinede: 26.3 penceresi sRGB destekli bir framebuffer istiyor,
# Xvfb'nin yazılım OpenGL'i bunu sunmadığı için oyun Vulkan'a geçer. Mesa'nın
# yazılım Vulkan sürücüsü (lavapipe) yeterli:
#   sudo apt-get install xvfb mesa-vulkan-drivers libvulkan1
xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGametest
```

---

## Lisans

Kod MIT lisanslıdır — `LICENSE` dosyasına bak.

`assets/realcars/sounds/engine/` altındaki motor sesleri Freesound'daki CC0 ve
CC BY lisanslı kayıtlardan türetilmiştir; yazarları CREDITS.md'de.

`assets/realcars/hd/` altındaki araç modelleri ve
`assets/realcars/textures/entity/car/hd/` altındaki dokuları, özgün yazarlarının
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) lisansıyla
yayımladığı modellerden türetilmiştir. Modu dağıtırken [CREDITS.md](CREDITS.md)
dosyasındaki atıfları korumalısın.
