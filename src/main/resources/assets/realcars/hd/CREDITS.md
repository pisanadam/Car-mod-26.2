# Atıflar

## 3B araç modelleri

Araçların yüksek poligonlu gövdeleri, aşağıdaki sanatçıların Sketchfab'da
[Creative Commons Atıf 4.0](https://creativecommons.org/licenses/by/4.0/) lisansıyla yayımladığı modellerden
dönüştürülmüştür. Modeller, lisans bilgisiyle birlikte
[Objaverse](https://huggingface.co/datasets/allenai/objaverse) arşivinden indirildi.

Yapılan değişiklikler (hepsi `tools/import_hd_models.py` ile): ölçek ve yön
araç boyuna göre ayarlandı; çokgen sayısı azaltıldı (yakın görünüm en fazla
~70000, uzak görünüm ~9000 üçgen); dokular tek bir atlasa toplandı; boya
malzemesi oyunda yeniden boyanabilsin diye griye çevrildi; tekerlekler dönebilsin
diye gövdeden ayrıldı; sahne parçaları (zemin gölgesi vb.) çıkarıldı.

Marka ve model adları ilgili üreticilerin ticari markalarıdır; modellerin
yazarları ve bu mod o üreticilerle bağlantılı değildir.

| Araç | Model | Yazar | Lisans |
|---|---|---|---|
| `golf_gti` | [Volkswagen Golf Gti 16V](https://sketchfab.com/3d-models/877b82ed48954e08a234dae968b70704) | LePointBAT | CC BY 4.0 |
| `corolla` | [Toyota Corolla AE86 Trueno](https://sketchfab.com/3d-models/fe02fba6302e450ea8424591493341ea) | Lexyc16 | CC BY 4.0 |
| `bmw_m3` | [BMW M3 Coupe (E30) 1986](https://sketchfab.com/3d-models/8fa21fe97a6042a2a09e0b09fd546b91) | Lexyc16 | CC BY 4.0 |
| `mustang_gt` | ['67 Mustang (High poly)](https://sketchfab.com/3d-models/4223fcc87baa4ac58be2910a5367d7d0) | nickbroad | CC BY 4.0 |
| `gtr` | [Nissan Skyline R32 GT-R](https://sketchfab.com/3d-models/8810e677eca543a6b840cf67dd064aa4) | Lexyc16 | CC BY 4.0 |
| `porsche_911` | [FREE 1975 Porsche 911 (930) Turbo](https://sketchfab.com/3d-models/8568d9d14a994b9cae59499f0dbed21e) | karolmiklas | CC BY 4.0 |
| `wrangler` | [Jeep Rubicon](https://sketchfab.com/3d-models/178f22467bae4c729bdcc15dbc7e445d) | Armawiz | CC BY 4.0 |
| `hilux` | [Toyota Hilux 2007](https://sketchfab.com/3d-models/0234ebed01ee4b87affed0e58673247e) | David_Holiday | CC BY 4.0 |
| `defender` | [2021 Land Rover Defender 110](https://sketchfab.com/3d-models/26f042fcf1594890a529bfb41f0fae0d) | David_Holiday | CC BY 4.0 |
| `beetle` | [Beetle/Fusca (Version 1)](https://sketchfab.com/3d-models/cb6e82e73386448fb3b4e2dd25255a5e) | soujagah | CC BY 4.0 |
| `c10` | [1983 Chevrolet C10](https://sketchfab.com/3d-models/14116da06a65420db32b585466061d31) | korsun | CC BY 4.0 |
| `transporter` | [Volkswagen Kombi Micro Bus](https://sketchfab.com/3d-models/d34fd744db1c4f84ad40c854ddbff9e9) | winterborn | CC BY 4.0 |

## Motor sesleri

Motor sesleri aşağıdaki gerçek kayıtlardan üretilmiştir (Freesound; Hugging Face'teki
[lisans bilgili aynasından](https://huggingface.co/datasets/benjamin-paine/freesound-laion-640k) indirildi).
Her kayıttan rölanti, orta ve yüksek devir döngüleri kesildi, perdesi harmonik çizgi
izlenerek sabitlendi ve döngü noktası çapraz geçişle birleştirildi; marş sesleri de
kayıtların kendi marş anlarından kesildi
(`tools/import_engine_sounds.py`).

| Ses | Kayıt | Yazar | Lisans |
|---|---|---|---|
| `golf_gti` | [SFX Car Engine Outside Idle](https://freesound.org/s/401552/) | GiocoSound | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `golf_gti` | [SFX Car Engine Outside RPMMed](https://freesound.org/s/401555/) | GiocoSound | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `golf_gti` | [SFX Car Engine Outside RPMHigh](https://freesound.org/s/401551/) | GiocoSound | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `golf_gti` | [SFX Car Engine Outside Start](https://freesound.org/s/401558/) | GiocoSound | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `golf_gti` | [SFX Car Engine Outside Stop](https://freesound.org/s/401557/) | GiocoSound | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `corolla` | [4A-GE Engine Starting and Loop](https://freesound.org/s/509628/) | qopsinonstudios | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
| `corolla` | [4A-GE Engine Loop 1](https://freesound.org/s/509626/) | qopsinonstudios | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `corolla` | [4A-GE Engine Loop 2](https://freesound.org/s/509627/) | qopsinonstudios | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `bmw_m3` | [BMWM3 01](https://freesound.org/s/415276/) | ikbenraar | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
| `mustang_gt` | [ford mustang v8 rear 2](https://freesound.org/s/384058/) | pastori666 | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `mustang_gt` | [Starting of Ford V8 5 Liter engine](https://freesound.org/s/455925/) | noiseloop | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `gtr` | [1953 Studebaker Idle up to 6000rpm](https://freesound.org/s/238847/) | lonemonk | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `porsche_911` | [Porsche idle](https://freesound.org/s/113216/) | victormalloy | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `porsche_911` | [Porsche Exhaust](https://freesound.org/s/113214/) | victormalloy | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `porsche_911` | [Ignition Porsche growl](https://freesound.org/s/113200/) | victormalloy | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `porsche_911` | [Porsche off growl](https://freesound.org/s/113217/) | victormalloy | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `wrangler` | [Jeep in the Mountians #1](https://freesound.org/s/253439/) | deleted_user_4401185 | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `v6`, `wrangler` | [Vitara V6 engine](https://freesound.org/s/249720/) | Keith Selmes | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `hilux` | [Diesel Start-Idle-Revs MZ000004](https://freesound.org/s/443319/) | DAVESTALKER | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `defender` | [Renault Master F3500 dCi135 Engine Start Up Idle Gas Pressing And Shut Down Mono](https://freesound.org/s/425845/) | Soundholder | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `defender` | [diesel starting and idling](https://freesound.org/s/264870/) | paulmessier | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
| `beetle` | [Volkswagen Beetle 1961](https://freesound.org/s/544344/) | klankbeeld | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
| `beetle` | [1970 VW Bug Engine Starting Sputtering](https://freesound.org/s/579211/) | bowlingballout | [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/) |
| `c10` | [Chev 350 rev and idle](https://freesound.org/s/123685/) | lonemonk | [CC-BY-3.0](https://creativecommons.org/licenses/by/3.0/) |
| `transporter` | [Volkswagen Type 2 revving up2](https://freesound.org/s/332695/) | nhaudio | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
| `transporter` | [Volkswagen Type 2 1 starting](https://freesound.org/s/275403/) | nhaudio | [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/) |
