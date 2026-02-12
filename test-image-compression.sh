#!/bin/bash

# ============================================================
# Test: Periodična kompresija thumbnail slika (starijih od 30 dana)
# ============================================================

BASE="http://localhost:8081"

echo "=== 1. INFO O KONFIGURACIJI ==="
curl -s "$BASE/api/compression/info" | python3 -m json.tool
echo ""

echo "=== 2. STATISTIKA PRIJE TESTA ==="
curl -s "$BASE/api/compression/stats" | python3 -m json.tool
echo ""

echo "=== 3. SIMULACIJA: Postavljam created_at na 31 dan unazad ==="
echo "(Scheduler trazi slike starije od 30 dana - simuliramo stare videe)"
psql -U postgres -d jutjubic_db -c \
  "UPDATE public.videos SET created_at = NOW() - INTERVAL '31 days', thumbnail_compressed = false, thumbnail_compressed_path = null, thumbnail_compression_date = null;"
echo "Done."
echo ""

echo "=== 4. STATISTIKA NAKON SIMULACIJE ==="
echo "(eligibleForCompression treba biti > 0)"
curl -s "$BASE/api/compression/stats" | python3 -m json.tool
echo ""

echo "=== 5. VELICINE ORIGINALNIH SLIKA (prije kompresije) ==="
find ./uploads/thumbnails -maxdepth 1 -type f | while read f; do
  size=$(du -h "$f" | cut -f1)
  echo "  $size  $(basename $f)"
done
echo ""

echo "=== 6. POKRECEM KOMPRESIJU (simulira dnevni scheduler u ponoc) ==="
curl -s -X POST "$BASE/api/compression/trigger" | python3 -m json.tool
echo ""

echo "Cekam da se kompresija zavrsi..."
sleep 3
echo ""

echo "=== 7. STATISTIKA NAKON KOMPRESIJE ==="
curl -s "$BASE/api/compression/stats" | python3 -m json.tool
echo ""

echo "=== 8. VELICINE KOMPRESOVANIH SLIKA ==="
if [ -d "./uploads/thumbnails/compressed" ]; then
  find ./uploads/thumbnails/compressed -type f | while read f; do
    size=$(du -h "$f" | cut -f1)
    echo "  $size  $(basename $f)"
  done
else
  echo "  (nema kompresovanih slika)"
fi
echo ""

echo "=== 9. ORIGINAL vs KOMPRESOVANO - poređenje velicina ==="
for orig in ./uploads/thumbnails/*.jpeg ./uploads/thumbnails/*.jpg ./uploads/thumbnails/*.png; do
  [ -f "$orig" ] || continue
  name=$(basename "$orig")
  noext="${name%.*}"
  comp="./uploads/thumbnails/compressed/${noext}_compressed.jpg"
  if [ -f "$comp" ]; then
    orig_size=$(du -k "$orig" | cut -f1)
    comp_size=$(du -k "$comp" | cut -f1)
    usteda=$(( (orig_size - comp_size) * 100 / orig_size ))
    echo "  $name: ${orig_size}KB -> ${comp_size}KB (usteda: ${usteda}%)"
  fi
done
echo ""

echo "=== 10. PROVJERA U BAZI (thumbnail_compressed = true) ==="
psql -U postgres -d jutjubic_db -c \
  "SELECT id, title, thumbnail_compressed, thumbnail_compression_date FROM public.videos ORDER BY id;"

echo ""
echo "=== 11. PROVJERA: Originali su SACUVANI (ne smiju biti obrisani) ==="
orig_count=$(find ./uploads/thumbnails -maxdepth 1 -type f | wc -l | tr -d ' ')
comp_count=$(find ./uploads/thumbnails/compressed -type f 2>/dev/null | wc -l | tr -d ' ')
echo "  Originalnih slika: $orig_count (moraju ostati!)"
echo "  Kompresovanih kopija: $comp_count"

echo ""
echo "=== KRAJ TESTA ==="
