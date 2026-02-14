#!/bin/bash

echo "🚀 Pokretanje Prometheus i Grafana..."
echo ""

echo "📡 Kreiranje Docker network..."
docker network create monitoring 2>/dev/null && echo "✅ Network 'monitoring' kreiran" || echo "✓ Network 'monitoring' već postoji"
echo ""

echo "🧹 Čišćenje starih kontejnera..."
docker rm -f prometheus grafana 2>/dev/null && echo "✅ Stari kontejneri uklonjeni" || echo "✓ Nema starih kontejnera"
echo ""

echo "📊 Pokrećem Prometheus..."
docker run -d \
  --name prometheus \
  --network monitoring \
  --add-host=host.docker.internal:host-gateway \
  -p 9090:9090 \
  -v "$(pwd)/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml" \
  --restart unless-stopped \
  prom/prometheus:latest \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus

if [ $? -eq 0 ]; then
    echo "✅ Prometheus pokrenut"
else
    echo "❌ Greška pri pokretanju Prometheus-a"
    echo "   Proveri: docker logs prometheus"
    exit 1
fi
echo ""

sleep 2

echo "📈 Pokrećem Grafana..."
docker run -d \
  --name grafana \
  --network monitoring \
  -p 3001:3000 \
  -e "GF_SECURITY_ADMIN_USER=admin" \
  -e "GF_SECURITY_ADMIN_PASSWORD=admin" \
  -e "GF_USERS_ALLOW_SIGN_UP=false" \
  -e "GF_AUTH_ANONYMOUS_ENABLED=true" \
  -e "GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer" \
  -v "$(pwd)/grafana/provisioning:/etc/grafana/provisioning" \
  -v "$(pwd)/grafana/dashboards:/var/lib/grafana/dashboards" \
  --restart unless-stopped \
  grafana/grafana:latest

if [ $? -eq 0 ]; then
    echo "✅ Grafana pokrenuta"
else
    echo "❌ Greška pri pokretanju Grafane"
    echo "   Proveri: docker logs grafana"
    exit 1
fi
echo ""

echo "⏳ Čekam da se servisi pokrenu (10 sekundi)..."
sleep 10
echo ""

echo "🔍 Provera statusa kontejnera..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(NAMES|prometheus|grafana)"
echo ""

echo "🧪 Testiranje pristupa..."

if curl -s http://localhost:9090/-/ready > /dev/null 2>&1; then
    echo "✅ Prometheus je dostupan na http://localhost:9090"
else
    echo "⚠️  Prometheus još nije spreman (može biti da treba još malo vremena)"
fi

if curl -s http://localhost:3001/api/health > /dev/null 2>&1; then
    echo "✅ Grafana je dostupna na http://localhost:3001"
else
    echo "⚠️  Grafana još nije spremna (može biti da treba još malo vremena)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ MONITORING STACK POKRENUT!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Prometheus: http://localhost:9090"
echo "📈 Grafana:    http://localhost:3001"
echo ""
echo "🔐 Grafana credentials:"
echo "   Username: admin"
echo "   Password: admin"
echo ""
echo "📝 Komande za upravljanje:"
echo "   Zaustavi:  docker stop prometheus grafana"
echo "   Pokreni:   docker start prometheus grafana"
echo "   Logovi:    docker logs -f grafana"
echo "   Ukloni:    docker rm -f prometheus grafana"
echo ""
echo "💡 TIP: Otvori http://localhost:3001 u browseru"
echo ""
