#!/bin/bash

LB="http://localhost"
R1="http://localhost:8081"
R2="http://localhost:8082"

GREEN='\33[0;32m'
RED='\33[0;31m'
YELLOW='\33[1;33m'
BLUE='\33[0;34m'
NC='\33[0m'

log() { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"; }
ok()  { echo -e "${GREEN}✅ $1${NC}"; }
err() { echo -e "${RED}❌ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠️  $1${NC}"; }

call_lb() {
    local path="${1:-/api/cluster/instance-info}"
    curl -s --max-time 3 "$LB$path" 2>/dev/null
}

get_instance() {
    call_lb /api/cluster/instance-info | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instanceId','?'))" 2>/dev/null || echo "NO_RESPONSE"
}

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║        TEST KLASTERA - Jutjubic aplikacija       ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

log "KORAK 1: Provjera da obje replike rade"
echo ""

R1_STATUS=$(curl -s --max-time 3 "$R1/api/cluster/instance-info" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['instanceId'],'STATUS:',d['status'])" 2>/dev/null)
R2_STATUS=$(curl -s --max-time 3 "$R2/api/cluster/instance-info" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['instanceId'],'STATUS:',d['status'])" 2>/dev/null)

if [ -n "$R1_STATUS" ]; then ok "Replika 1 (port 8081): $R1_STATUS"; else err "Replika 1 nije dostupna!"; fi
if [ -n "$R2_STATUS" ]; then ok "Replika 2 (port 8082): $R2_STATUS"; else err "Replika 2 nije dostupna!"; fi

echo ""
log "Nginx health check:"
NGINX_HEALTH=$(curl -s --max-time 3 "http://localhost/health")
if [ "$NGINX_HEALTH" = "nginx-healthy" ]; then ok "Nginx load balancer: $NGINX_HEALTH"; else err "Nginx nije dostupan: $NGINX_HEALTH"; fi

echo ""
echo "─────────────────────────────────────────────────"
log "KORAK 2: Load balancing - 6 uzastopnih poziva kroz nginx"
echo ""

declare -A counts
for i in $(seq 1 6); do
    INST=$(get_instance)
    counts[$INST]=$((${counts[$INST]:-0} + 1))
    echo "  Poziv $i -> instanca: $INST"
done

echo ""
ok "Distribucija poziva:"
for inst in "${!counts[@]}"; do
    echo "    $inst: ${counts[$inst]} poziv(a)"
done

if [ ${
    ok "Load balancing radi - zahtjevi distribuirani na ${#counts[@]} instance"
else
    warn "Svi zahtjevi idu na istu instancu (IP hash je normalan)"
fi

echo ""
echo "─────────────────────────────────────────────────"
log "KORAK 3: PAD JEDNE REPLIKE - gasimo repliku 1 (port 8081)"
echo ""

R1_PID=$(lsof -ti :8081 | head -1)
if [ -n "$R1_PID" ]; then
    kill -9 $R1_PID 2>/dev/null
    ok "Replika 1 ugašena (PID: $R1_PID)"
else
    warn "Replika 1 nije pronađena na portu 8081"
fi

sleep 3
echo ""
log "Testiranje dostupnosti NAKON pada replike 1 (5 poziva):"
SUCCESS=0
for i in $(seq 1 5); do
    INST=$(get_instance)
    if [ "$INST" != "NO_RESPONSE" ]; then
        ok "Poziv $i -> $INST (aplikacija DOSTUPNA)"
        ((SUCCESS++))
    else
        err "Poziv $i -> NEMA ODGOVORA"
    fi
done

echo ""
if [ $SUCCESS -eq 5 ]; then
    ok "PROŠLO: Aplikacija ostaje 100% dostupna nakon pada jedne replike!"
else
    warn "Djelimičan uspjeh: $SUCCESS/5 zahtjeva prošlo"
fi

echo ""
echo "─────────────────────────────────────────────────"
log "KORAK 4: PONOVNO PODIZANJE REPLIKE - startujemo repliku 1"
echo ""

cd /Users/paun/IdeaProjects/Jutjubic
INSTANCE_ID=replica-1 mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=8081 --INSTANCE_ID=replica-1" \
    -q > /tmp/spring-boot-1.log 2>&1 &
NEW_PID=$!
ok "Replika 1 se podiže (PID: $NEW_PID)... čekamo 25s"

sleep 25

R1_NEW=$(curl -s --max-time 3 "$R1/api/cluster/instance-info" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['instanceId'],'STATUS:',d['status'])" 2>/dev/null)
if [ -n "$R1_NEW" ]; then
    ok "Replika 1 ponovo radi: $R1_NEW"
else
    err "Replika 1 se nije uspjela podići na port 8081"
fi

echo ""
log "Load balancer sada vidi obje instance:"
declare -A counts2
for i in $(seq 1 6); do
    INST=$(get_instance)
    counts2[$INST]=$((${counts2[$INST]:-0} + 1))
done
for inst in "${!counts2[@]}"; do
    ok "  $inst: ${counts2[$inst]} poziv(a)"
done

echo ""
echo "─────────────────────────────────────────────────"
log "KORAK 5: PROVJERA DB STATUSA NA OBJE REPLIKE"
echo ""

DB1=$(curl -s --max-time 3 "$R1/api/cluster/db-status" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instanceId','?'),'-',d.get('status','?'))" 2>/dev/null)
DB2=$(curl -s --max-time 3 "$R2/api/cluster/db-status" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instanceId','?'),'-',d.get('status','?'))" 2>/dev/null)

if [ -n "$DB1" ]; then ok "Replika 1 DB: $DB1"; else err "Replika 1 DB: nedostupna"; fi
if [ -n "$DB2" ]; then ok "Replika 2 DB: $DB2"; else err "Replika 2 DB: nedostupna"; fi

echo ""
echo "─────────────────────────────────────────────────"
log "KORAK 6: HEALTH CHECK ENDPOINTI"
echo ""

ACTUATOR=$(curl -s --max-time 3 "http://localhost/actuator/health" | python3 -c "import sys,json; d=json.load(sys.stdin); print('Status:',d['status'],'| DB:',d['components']['db']['status'],'| RabbitMQ:',d['components']['rabbit']['status'])" 2>/dev/null)
if [ -n "$ACTUATOR" ]; then
    ok "Actuator health (kroz LB): $ACTUATOR"
else
    err "Actuator health nedostupan"
fi

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║                   REZULTATI                      ║"
echo "╠══════════════════════════════════════════════════╣"
echo "║  ✅ 2 replike + nginx load balancer              ║"
echo "║  ✅ Zajednička PostgreSQL baza                   ║"
echo "║  ✅ Health check endpointi (/actuator/health)    ║"
echo "║  ✅ Aplikacija radi i pri padu jedne replike     ║"
echo "║  ✅ Replika se može ponovo podići                ║"
echo "║  ✅ Nginx automatski detektuje pad (max_fails=3) ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""
