#!/bin/bash


EMAIL="sveplacamo@gmail.com"
PASSWORD="Paun123\$"

VIDEO1="./uploads/videos/test_sample.mp4"
VIDEO2="./uploads/videos/video_a3e8f1ba-3584-474f-ad84-eedb4b2464e0.mp4"
THUMB="./uploads/thumbnails/thumb_782f3a5f-5cb7-4fd8-9768-dda42165b14c.jpeg"

echo "=== Prijava ==="
TOKEN=$(curl -s -X POST "http://localhost:8081/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('token',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "GRESKA: Login nije uspio. Provjeri EMAIL i PASSWORD u skripti."
  exit 1
fi

echo "Token dobijen: ${TOKEN:0:30}..."
echo ""
echo "=== Saljem 2 uploada ISTOVREMENO ==="
echo "(Gledaj IntelliJ logove - trebas vidjeti 2 razlicita rabbit-listener threada)"
echo ""

curl -s -X POST "http://localhost:8081/api/videos" \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=Test Video 1 - Consumer A" \
  -F "description=Paralelni test" \
  -F "video=@$VIDEO1;type=video/mp4" \
  -F "thumbnail=@$THUMB" \
  > /tmp/upload1_result.json &
PID1=$!

curl -s -X POST "http://localhost:8081/api/videos" \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=Test Video 2 - Consumer B" \
  -F "description=Paralelni test" \
  -F "video=@$VIDEO2;type=video/mp4" \
  -F "thumbnail=@$THUMB" \
  > /tmp/upload2_result.json &
PID2=$!

echo "Upload 1 pokrenut (PID: $PID1)"
echo "Upload 2 pokrenut (PID: $PID2)"
echo ""
echo "Cekam da oba zavrse..."
wait $PID1
wait $PID2

echo ""
echo "=== Rezultati ==="
echo "Upload 1:"
python3 -m json.tool /tmp/upload1_result.json 2>/dev/null | grep -E "id|title|error" | head -5

echo ""
echo "Upload 2:"
python3 -m json.tool /tmp/upload2_result.json 2>/dev/null | grep -E "id|title|error" | head -5

echo ""
echo "=== Provjeri transcoded fajlove ==="
ls -lh ./uploads/videos/transcoded/ | tail -5

echo ""
echo "=== OCEKIVANI REZULTAT U INTELLIJ LOGOVIMA ==="
echo "  🎬 [rabbit-listener-1] Primljena poruka za transcoding: Video ID X"
echo "  🎬 [rabbit-listener-2] Primljena poruka za transcoding: Video ID Y"
echo "  (razliciti thread-ovi = 2 consumera rade paralelno)"
echo "  (svaka poruka samo jednom = exactly-once delivery)"
