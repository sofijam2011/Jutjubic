#!/bin/bash

echo "🧪 Testiranje Docker DNS konfiguracije..."
echo ""

# Test 1: Proveri Docker daemon
echo "1. Provera Docker daemon-a..."
if docker info > /dev/null 2>&1; then
    echo "   ✅ Docker daemon radi"
else
    echo "   ❌ Docker daemon ne radi! Pokreni Docker Desktop."
    exit 1
fi
echo ""

# Test 2: Pokušaj preuzimanja test slike (glavni test)
echo "2. Pokušaj preuzimanja test slike (alpine)..."
PULL_OUTPUT=$(docker pull alpine:latest 2>&1)
PULL_EXIT=$?

if [ $PULL_EXIT -eq 0 ]; then
    echo "   ✅ Docker može da preuzima slike sa Docker Hub-a!"

    # Test 3: DNS lookup unutar kontejnera
    echo ""
    echo "3. Test DNS lookup (registry-1.docker.io)..."
    if docker run --rm alpine nslookup registry-1.docker.io > /dev/null 2>&1; then
        echo "   ✅ DNS radi unutar Docker kontejnera"
    else
        echo "   ⚠️  DNS lookup ne uspeva (ali preuzimanje slika radi)"
    fi

    # Test 4: Internet konekcija
    echo ""
    echo "4. Test internet konekcije..."
    if docker run --rm alpine ping -c 2 google.com > /dev/null 2>&1; then
        echo "   ✅ Internet konekcija radi"
    else
        echo "   ⚠️  Ping ne uspeva (ali preuzimanje slika radi)"
    fi

    echo ""
    echo "🎉 SVE RADI! Sada možeš preuzeti Prometheus i Grafana:"
    echo ""
    echo "   docker pull prom/prometheus:latest"
    echo "   docker pull grafana/grafana:latest"
    echo ""
    echo "   Ili pokreni: ./start-monitoring.sh"
else
    echo "   ❌ Docker ne može da preuzima slike"
    echo ""
    echo "Detalji greške:"
    echo "$PULL_OUTPUT" | sed 's/^/   /'
    echo ""
    echo "Mogući uzroci:"
    if echo "$PULL_OUTPUT" | grep -q "no such host"; then
        echo "  - ❌ DNS problema - Docker ne može da prepozna registry-1.docker.io"
        echo "  - Proveri: cat ~/.docker/daemon.json"
        echo "  - Dodaj DNS: {\"dns\": [\"8.8.8.8\", \"8.8.4.4\"]}"
    fi
    if echo "$PULL_OUTPUT" | grep -q "timeout"; then
        echo "  - ❌ Timeout - moguće da nemaš internet ili firewall blokira"
    fi
    echo "  - Da li je Docker restartovan nakon izmene daemon.json?"
    echo "  - Da li imaš internet konekciju?"
fi
echo ""
