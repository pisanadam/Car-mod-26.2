#!/usr/bin/env bash
# RealCars başsız sunucu duman testi.
#
# Geliştirme sunucusunu düz bir dünyada başlatır, modun her aracını gerçekten
# spawn edip edemediğini ve her item'ın verilebilir olduğunu komutlarla dener,
# sonra günlükte kayıt/mixin/eksik doku hatası arar.
#
# Kullanım: JAVA_HOME=/opt/jdk25 tools/smoke_test.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/run"
LOG="${SMOKE_LOG:-$RUN/smoke.log}"
FIFO="$RUN/smoke.stdin"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-420}"

mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
cat > "$RUN/server.properties" <<'PROPS'
level-type=minecraft\:flat
online-mode=false
spawn-protection=0
max-tick-time=-1
level-name=smoketest
PROPS

# Önceki koşudan kalan dünya ve kilit dosyası testi baştan bozar.
rm -rf "$RUN/smoketest"
rm -f "$FIFO" "$LOG"
mkfifo "$FIFO"

# FIFO'yu açık tutan bir yazıcı; olmazsa sunucu ilk komuttan sonra EOF görür.
sleep infinity > "$FIFO" &
HOLDER=$!

( cd "$ROOT" && ./gradlew runServer --no-daemon --console=plain < "$FIFO" > "$LOG" 2>&1 ) &
GRADLE=$!

cleanup() {
	kill "$HOLDER" 2>/dev/null
	kill "$GRADLE" 2>/dev/null
	rm -f "$FIFO"
}
trap cleanup EXIT

send() {
	printf '%s\n' "$1" > "$FIFO"
}

echo "Sunucu başlatılıyor (en fazla ${BOOT_TIMEOUT}s)..."
waited=0
until grep -q 'Done (' "$LOG" 2>/dev/null; do
	if ! kill -0 "$GRADLE" 2>/dev/null; then
		echo "HATA: sunucu açılmadan sonlandı."
		tail -40 "$LOG"
		exit 1
	fi
	if [ "$waited" -ge "$BOOT_TIMEOUT" ]; then
		echo "HATA: sunucu ${BOOT_TIMEOUT}s içinde açılmadı."
		tail -40 "$LOG"
		exit 1
	fi
	sleep 3
	waited=$((waited + 3))
done
echo "Sunucu açıldı (${waited}s)."

# --- her aracı spawn et -------------------------------------------------
CARS=$(python3 - <<'PY'
import pathlib, sys
sys.path.insert(0, str(pathlib.Path("tools")))
import spec
print(" ".join(spec.car_item_id(c) for c in spec.CARS))
PY
)

send "gamerule sendCommandFeedback true"
for car in $CARS; do
	send "summon realcars:$car 0 5 0"
done
sleep 4

# --- her item'ı bir sahte oyuncu envanterine koymayı dene ---------------
# Oyuncu olmadığı için /give çalışmaz; onun yerine item kaydını doğrudan
# doğrulayan bir yol olarak loot komutu kullanılır.
ITEMS=$(python3 - <<'PY'
import pathlib, sys
sys.path.insert(0, str(pathlib.Path("tools")))
import spec
print(" ".join(spec.all_item_ids()))
PY
)
for item in $ITEMS; do
	send "loot spawn 0 5 0 loot {\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"realcars:$item\"}]}]}"
done
sleep 4

# --- tarif komutu -------------------------------------------------------
# Konsolda çeviri dosyası olmadığı için başlıklar ham anahtar olarak düşer;
# ızgara satırları ise düz metin, onları arayarak komutun çalıştığı görülür.
send "cars recipes asphalt"
send "cars recipes rubber"
send "cars recipes"
sleep 3

send "stop"

echo "Sunucunun kapanması bekleniyor..."
waited=0
while kill -0 "$GRADLE" 2>/dev/null && [ "$waited" -lt 120 ]; do
	sleep 3
	waited=$((waited + 3))
done
kill "$HOLDER" 2>/dev/null

# --- sonuçları değerlendir ---------------------------------------------
echo
echo "===== SONUÇ ====="
spawned=$(grep -c 'Summoned new' "$LOG")
expected=$(echo "$CARS" | wc -w)
echo "Spawn edilen araç: $spawned / $expected"

fail=0
if [ "$spawned" -ne "$expected" ]; then
	echo "HATA: bazı araçlar spawn edilemedi."
	grep -iE 'Unable to summon|No entity type' "$LOG" | head -10
	fail=1
fi

# /cars recipes gerçekten ızgara bastı mı?
if grep -qF '[A][A][A]' "$LOG"; then
	echo "Tarif komutu: asfalt ızgarası basıldı."
else
	echo "HATA: /cars recipes asphalt ızgara basmadı."
	grep -iE 'cars recipes|Unknown command' "$LOG" | head -5
	fail=1
fi
# Konsol çeviriyi çözebiliyorsa metin, çözemiyorsa ham anahtar düşer; ikisi de olur.
if grep -qE 'command\.realcars\.recipes\.shapeless|Order does not matter' "$LOG"; then
	echo "Tarif komutu: şekilsiz tarif (kauçuk) listelendi."
else
	echo "HATA: /cars recipes rubber şekilsiz tarifi listelemedi."
	fail=1
fi

# Kayıt, mixin ve kaynak hataları
# 'No key layers' düz dünya jeneratörünün boş ayarından gelen vanilla gürültüsü,
# modla ilgisi yok; onun dışındaki her hata satırı testi düşürür.
problems=$(grep -inE 'Unregistered|Missing model|Unable to load|Failed to (load|create|parse)|Registry .*(error|conflict)|Mixin apply failed|Exception|ERROR\]' "$LOG" \
	| grep -viE 'Advancement|Unable to load registries from world|SLF4J|Skipping|deprecat|No key layers' | head -20)
if [ -n "$problems" ]; then
	echo
	echo "Günlükte dikkat çeken satırlar:"
	echo "$problems"
	fail=1
fi

# Modun kendi uyarıları (örneğin doku bölgesine sığmayan gövde parçası)
warnings=$(grep -F 'doku bölgesine sığmıyor' "$LOG" | head -10)
if [ -n "$warnings" ]; then
	echo
	echo "Model uyarıları:"
	echo "$warnings"
	fail=1
fi

if [ "$fail" -eq 0 ]; then
	echo "Duman testi başarılı — tüm araçlar spawn oldu, günlük temiz."
fi
echo "Tam günlük: $LOG"
exit "$fail"
