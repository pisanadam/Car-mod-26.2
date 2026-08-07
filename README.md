# RealCars — Minecraft 26.2 Araba Modu

Gerçek hayattaki arabaları Minecraft'a getiren bir Fabric modu: parçalardan araba
craftlarsın, motoru çalıştırıp sürersin, km/saat göstergesinden hızını görürsün,
tekerlekler döner, toprakta ve kumda arkandan toz kalkar, garajda rengini,
tekerleğini ve rüzgarlığını değiştirirsin.

| | |
|---|---|
| **Minecraft** | 26.2 (Java Edition) |
| **Mod yükleyici** | Fabric Loader 0.19.3+ |
| **Gerekli** | Fabric API 0.156.0+26.2 |
| **Java** | 25 (Minecraft 26.x zorunlu kılıyor) |

---

## Kurulum

1. [Fabric Loader](https://fabricmc.net/use/) 0.19.3 veya üstünü Minecraft 26.2 için kur.
2. [Fabric API](https://modrinth.com/mod/fabric-api) sürüm `0.156.0+26.2`'yi indir.
3. Bu modun `realcars-1.0.0.jar` dosyasını ve Fabric API'yi `.minecraft/mods`
   klasörüne at.
4. Oyunu Fabric profiliyle başlat.

Kaynaktan derlemek için: `JAVA_HOME=<jdk25> ./gradlew build` → `build/libs/realcars-1.0.0.jar`

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

Tam listeyi oyun içindeki **tarif kitabından** görebilirsin — hepsi normal tarif
olarak kayıtlı.

### 2. Arabayı craftla

Bütün arabalar aynı 3×3 kalıbı kullanır:

```
tekerlek | ön cam  | tekerlek
motor    | şasi    | şanzıman
tekerlek | koltuk  | tekerlek
```

Hangi arabanın çıkacağını **şasi + motor + şanzıman + tekerlek** bileşimi
belirler. Örneğin spor şasi + V8 + manuel + spor tekerlek = **Ford Mustang GT**.

### 3. Sür

Arabayı yere koy (bir bloğun üstüne sağ tıkla), üzerine sağ tıklayarak bin.

| Tuş | İşlev |
|---|---|
| `W` / `S` | Gaz / fren — dururken `S` geri vitese alır |
| `A` / `D` | Direksiyon |
| `Shift` | El freni |
| `G` | Motoru çalıştır / durdur |
| `H` | Korna |
| `R` / `F` | Vites yükselt / düşür (manuel şanzımanda) |

Tuşlar oyun ayarlarından **RealCars** başlığı altında değiştirilebilir.

**Önce motoru çalıştırmayı unutma** (`G`) — yoksa araba hareket etmez.

### 4. Modifiye et

İki yol var:

- **İngiliz Anahtarı** elindeyken arabaya sağ tıkla, ya da
- **Araç Lifti** bloğunu arabanın yanına koyup lifte sağ tıkla.

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
| BMW M3 | Sedan | 290 km/s | 4.1 sn | I6 | Manuel | Spor |
| Ford Mustang GT | Spor | 250 km/s | 4.3 sn | V8 | Manuel | Spor |
| Nissan GT-R | Spor | 315 km/s | 2.9 sn | V6 | Spor DCT | Spor |
| Porsche 911 | Spor | 300 km/s | 3.4 sn | Boksör 6 | Spor DCT | Spor |
| Jeep Wrangler | Arazi | 180 km/s | 7.6 sn | V6 | Otomatik | Arazi |
| Toyota Hilux | Kamyonet | 175 km/s | 11.0 sn | I4 | Manuel | Arazi |
| Land Rover Defender | SUV | 190 km/s | 6.6 sn | I6 | Otomatik | Arazi |
| Volkswagen Beetle | Klasik | 130 km/s | 17.5 sn | I4 | Manuel | Sokak |
| Chevrolet C10 | Klasik kamyonet | 160 km/s | 9.0 sn | V8 | Manuel | Sokak |
| VW Transporter T1 | Minibüs | 105 km/s | 22.0 sn | I4 | Manuel | Sokak |

---

## Yol yapımı

**Asfalt**, **Asfalt Levha** ve iki renk **Yol Çizgisi** blokları var. Çizgiler
onları koyarken baktığın yöne göre uzanır.

Asfalt en yüksek tutuşu verir; taş ve beton onu izler; toprak, kum ve çakılda
tutuş düşer, buz ve karda daha da düşer. Yani gerçekten yol yapmak işe yarar:
aynı arabayla asfaltta çıkardığın hızı çimende çıkaramazsın.

---

## Bilmen gereken birkaç şey

- **Hız gerçek ölçekte.** 1 blok = 1 metre kabul edilir, dolayısıyla göstergedeki
  sayı oyundaki gerçek hızdır. Bunun bir sonucu var: 250 km/s saniyede ~69 blok
  demek ve bu, sunucunun chunk yükleme hızını zorlayabilir. Uzun yolculuklarda
  görüş mesafesini artırmak ya da yolu önceden yüklenmiş tutmak yardımcı olur.
- **Motor sesi devirle değişir.** Ses dosyaları sentezle üretildi; her motor tipi
  kendi ateşleme mertebesini vurguluyor, bu yüzden V8 ile 4 silindir kulakta
  gerçekten farklı duyuluyor.
- **Yanlış vites cezalandırır.** Manuel şanzımanda çok düşük ya da kırmızı bölge
  üstü devirde çekiş düşer; doğru anda vites değiştirmek gerçekten hızlandırır.
  Otomatik şanzıman bunu senin yerine yapar ama biraz güç kaybettirir.

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
python3 tools/gen_sounds.py       # motor sesleri (FFT sentezi) -> .ogg
python3 tools/gen_data.py         # tarifler, modeller, blockstate, dil dosyaları
python3 tools/validate_assets.py  # kırık referans / tarif çakışması / eksik çeviri kontrolü
```

**Yeni araç eklemek** iki dosyaya birer kayıt yazmaktır:

1. `tools/spec.py` → `CARS` tablosuna bir satır
2. `src/main/java/.../entity/CarModel.java` → aynı isimle bir enum sabiti (ölçüler
   ve fizik değerleriyle)

Sonra üreticileri çalıştır; doku, ikon, tarif, dil anahtarı ve 3B gövde
otomatik üretilir. `validate_assets.py` iki dosyanın uyumunu kontrol eder.

### Testler

```bash
JAVA_HOME=<jdk25> ./gradlew build     # derleme
python3 tools/validate_assets.py      # kaynak bütünlüğü
JAVA_HOME=<jdk25> tools/smoke_test.sh # başsız sunucuda 12 aracı spawn edip günlüğü tarar
```

---

## Lisans

MIT — `LICENSE` dosyasına bak.
