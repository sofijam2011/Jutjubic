#!/bin/bash
# Test skripta za ETL Pipeline API

BASE_URL="http://localhost:8081"

echo "================================="
echo "ETL PIPELINE API TESTIRANJE"
echo "================================="
echo ""

# 1. Prvo se uloguj da dobiješ JWT token
echo "1. Login kao admin korisnik..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Login failed! Response: $LOGIN_RESPONSE"
    exit 1
fi

echo "✅ Login successful! Token: ${TOKEN:0:20}..."
echo ""

# 2. Pokreni ETL Pipeline manuelno
echo "2. Pokretanje ETL Pipeline-a..."
ETL_RESPONSE=$(curl -s -X POST "$BASE_URL/api/popular-videos/run-etl" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "ETL Response: $ETL_RESPONSE"
echo ""

# Sačekaj malo da se pipeline završi
echo "Čekam 3 sekunde da se pipeline završi..."
sleep 3
echo ""

# 3. Preuzmi najpopularnije videe
echo "3. Preuzimanje top 3 popularna videa..."
POPULAR_VIDEOS=$(curl -s -X GET "$BASE_URL/api/popular-videos" \
  -H "Authorization: Bearer $TOKEN")

echo "Popular Videos Response:"
echo $POPULAR_VIDEOS | jq '.'
echo ""

# 4. Analiza rezultata
echo "================================="
echo "ANALIZA REZULTATA:"
echo "================================="
echo $POPULAR_VIDEOS | jq -r '.[] | "Rank \(.rankPosition): \(.title) - Score: \(.popularityScore) - Views: \(.viewCount)"'
echo ""

# 5. Proveri da li frontend može da učita podatke
echo "4. Test frontend poziva (bez autentifikacije)..."
curl -s -X GET "$BASE_URL/api/popular-videos" | jq '.' || echo "❌ Failed without auth"
echo ""

echo "================================="
echo "TEST ZAVRŠEN!"
echo "================================="
