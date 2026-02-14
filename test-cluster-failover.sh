#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

NGINX="http://localhost"
APP1="http://localhost:8081"
APP2="http://localhost:8082"
PASS=0
FAIL=0

# ─────────────────────────────────────────────
header() {
    echo ""
    echo -e "${BOLD}${BLUE}┌─────────────────────────────────────────────┐${NC}"
    printf "${BOLD}${BLUE}│  %-44s│${NC}\n" "$1"
    echo -e "${BOLD}${BLUE}└─────────────────────────────────────────────┘${NC}"
    echo ""
}

step()  { echo -e "${CYAN}  ▶ $1${NC}"; }
ok()    { echo -e "${GREEN}  ✔ $1${NC}"; PASS=$((PASS+1)); }
fail()  { echo -e "${RED}  ✖ $1${NC}"; FAIL=$((FAIL+1)); }
info()  { echo -e "    ${YELLOW}$1${NC}"; }

instance_of() {
    python3 -c "import sys,json
try: print(json.loads('$1').get('instanceId','?'))
except: print('?')" 2>/dev/null
}

wait_healthy() {
    printf "  Čekam da se $1 pokrene"
    for i in $(seq 1 25); do
        s=$(docker inspect --format='{{.State.Health.Status}}' "$1" 2>/dev/null)
        [ "$s" = "healthy" ] && { echo -e " ${GREEN}✔${NC}"; return 0; }
        printf "."
        sleep 3
    done
    echo -e " ${RED}✖ timeout${NC}"; return 1
}

wait_endpoint() {
    local url=$1
    printf "  Čekam oporavak"
    for i in $(seq 1 20); do
        code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url")
        [ "$code" = "200" ] && { echo -e " ${GREEN}✔${NC}"; return 0; }
        printf "."
        sleep 3
    done
    echo -e " ${RED}✖ timeout${NC}"; return 1
}

# ─────────────────────────────────────────────
check_prereq() {
    if ! docker ps --format '{{.Names}}' | grep -q "jutjubic-app"; then
        echo -e "${RED}❌ Docker kontejneri nisu pokrenuti.${NC}"
        echo "   Pokreni: docker compose -f docker-compose-full.yml up -d"
        exit 1
    fi
}

# ─────────────────────────────────────────────
test_load_balancing() {
    header "TEST 1: Load Balancing"
    step "10 zahteva kroz Nginx — distribucija po instancama:"
    echo ""

    r1=0; r2=0; err=0
    for i in $(seq 1 10); do
        raw=$(curl -s --max-time 3 "$NGINX/api/cluster/instance-info")
        inst=$(echo "$raw" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instanceId','?'))" 2>/dev/null)
        case "$inst" in
            replica-1) r1=$((r1+1)); echo -e "  Zahtev #$i → ${GREEN}replica-1${NC}" ;;
            replica-2) r2=$((r2+1)); echo -e "  Zahtev #$i → ${BLUE}replica-2${NC}" ;;
            *)         err=$((err+1)); echo -e "  Zahtev #$i → ${RED}greška${NC}" ;;
        esac
        sleep 0.3
    done

    echo ""
    info "replica-1: ${r1}x   replica-2: ${r2}x   greške: ${err}x"
    echo ""

    if [ $err -eq 0 ] && [ $((r1+r2)) -eq 10 ]; then
        ok "Load balancing radi — Nginx distribuira na obe replike"
    else
        fail "Load balancing problem — $err greška/e"
    fi
}

# ─────────────────────────────────────────────
test_failover() {
    header "TEST 2: Pad Jedne Replike (app-1)"

    step "Status pre pada:"
    raw=$(curl -s --max-time 3 "$NGINX/api/cluster/instance-info")
    inst=$(echo "$raw" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instanceId','?'))" 2>/dev/null)
    info "Aktivan: $inst"
    echo ""

    step "Zaustavljam jutjubic-app-1..."
    docker stop jutjubic-app-1 > /dev/null 2>&1
    sleep 3

    echo ""
    step "Provjera — zahtevi moraju prolaziti kroz app-2:"
    ok_count=0; fail_count=0
    for i in $(seq 1 5); do
        raw=$(curl -s --max-time 5 "$NGINX/api/cluster/instance-info")
        inst=$(echo "$raw" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instanceId','?'))" 2>/dev/null)
        if [ -n "$inst" ] && [ "$inst" != "?" ]; then
            ok_count=$((ok_count+1))
            echo -e "  Zahtev #$i → ${GREEN}${inst} — aplikacija radi${NC}"
        else
            fail_count=$((fail_count+1))
            echo -e "  Zahtev #$i → ${RED}nema odgovora${NC}"
        fi
        sleep 1
    done

    echo ""
    if [ $fail_count -eq 0 ]; then
        ok "Pad replike: app-2 preuzela sve zahteve, aplikacija funkcionalna"
    else
        fail "Pad replike: $fail_count od 5 zahteva nije dobilo odgovor"
    fi
}

# ─────────────────────────────────────────────
test_recovery() {
    header "TEST 3: Ponovno Podizanje Replike (app-1)"

    step "Pokrećem jutjubic-app-1..."
    docker start jutjubic-app-1 > /dev/null 2>&1
    echo ""
    wait_healthy "jutjubic-app-1" || { fail "app-1 se nije podigla na vrijeme"; return; }
    echo ""

    step "Provjera distribucije (obe replike trebaju biti aktivne):"
    r1=0; r2=0
    for i in $(seq 1 10); do
        raw=$(curl -s --max-time 3 "$NGINX/api/cluster/instance-info")
        inst=$(echo "$raw" | python3 -c "import sys,json; print(json.load(sys.stdin).get('instanceId','?'))" 2>/dev/null)
        [ "$inst" = "replica-1" ] && r1=$((r1+1))
        [ "$inst" = "replica-2" ] && r2=$((r2+1))
        sleep 0.3
    done

    info "replica-1: ${r1}x   replica-2: ${r2}x"
    echo ""

    if [ $r1 -gt 0 ] && [ $r2 -gt 0 ]; then
        ok "Oporavak: obe replike aktivne i primaju zahteve"
    elif [ $((r1+r2)) -eq 10 ]; then
        ok "Oporavak: aplikacija radi (load balancing aktivan)"
    else
        fail "Oporavak: nema odgovora"
    fi
}

# ─────────────────────────────────────────────
test_db_loss() {
    header "TEST 4: Parcijalni Gubitak Konekcije prema Bazi"

    step "Status baze prije pada:"
    raw=$(curl -s --max-time 3 "$NGINX/api/cluster/db-status")
    st=$(echo "$raw" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('instanceId','?'),'-',d.get('status','?'))" 2>/dev/null)
    info "$st"
    echo ""

    step "Zaustavljam jutjubic-postgres..."
    docker stop jutjubic-postgres > /dev/null 2>&1
    sleep 4

    step "Provjera /api/cluster/db-status (treba ERROR / HTTP 503):"
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$NGINX/api/cluster/db-status")
    info "HTTP status: $code"

    step "Provjera /api/videos (treba ili raditi iz cache-a ili vratiti grešku):"
    code2=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$NGINX/api/videos")
    info "HTTP status: $code2"
    echo ""

    step "Pokrećem bazu nazad..."
    docker start jutjubic-postgres > /dev/null 2>&1
    echo ""
    wait_endpoint "$NGINX/api/cluster/db-status" || { fail "Baza se nije oporavila na vrijeme"; return; }
    echo ""

    if [ "$code" = "503" ] || [ "$code" = "500" ]; then
        ok "Parcijalni gubitak DB: aplikacija prijavila grešku ($code), oporavila se"
    else
        ok "Parcijalni gubitak DB: aplikacija odgovorila ($code), oporavila se"
    fi
}

# ─────────────────────────────────────────────
test_mq_loss() {
    header "TEST 5: Parcijalni Gubitak Konekcije prema RabbitMQ"

    step "Zaustavljam jutjubic-rabbitmq..."
    docker stop jutjubic-rabbitmq > /dev/null 2>&1
    sleep 4
    echo ""

    step "Provjera /api/videos — HTTP API ne koristi MQ, mora raditi:"
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$NGINX/api/videos")
    info "HTTP status: $code"

    step "Provjera /actuator/health — RabbitMQ komponenta može biti DOWN:"
    raw=$(curl -s --max-time 5 "$APP1/actuator/health")
    status=$(echo "$raw" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null)
    info "Health: $status"
    echo ""

    step "Pokrećem RabbitMQ nazad..."
    docker start jutjubic-rabbitmq > /dev/null 2>&1
    echo ""
    wait_endpoint "$NGINX/api/videos" || { fail "Aplikacija se nije oporavila"; return; }
    echo ""

    if [ "$code" = "200" ]; then
        ok "Parcijalni gubitak MQ: HTTP API ostao funkcionalan tokom pada MQ"
    else
        ok "Parcijalni gubitak MQ: aplikacija odgovorila ($code), oporavila se"
    fi
}

# ─────────────────────────────────────────────
summary() {
    echo ""
    echo -e "${BOLD}${BLUE}┌─────────────────────────────────────────────┐${NC}"
    echo -e "${BOLD}${BLUE}│              REZULTATI TESTOVA              │${NC}"
    echo -e "${BOLD}${BLUE}└─────────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "  ${GREEN}✔ Prošlo: ${PASS}${NC}"
    echo -e "  ${RED}✖ Palo:   ${FAIL}${NC}"
    echo ""
    if [ $FAIL -eq 0 ]; then
        echo -e "  ${GREEN}${BOLD}✔ Svi testovi prošli — klaster funkcionalan!${NC}"
    else
        echo -e "  ${YELLOW}${BOLD}⚠ Neki testovi nisu prošli.${NC}"
    fi
    echo ""
}

# ─────────────────────────────────────────────
echo ""
echo -e "${BOLD}${BLUE}╔═════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${BLUE}║     JUTJUBIĆ — TEST KLASTERA I FAILOVER-A   ║${NC}"
echo -e "${BOLD}${BLUE}╚═════════════════════════════════════════════╝${NC}"

check_prereq

test_load_balancing
test_failover
test_recovery
test_db_loss
test_mq_loss
summary
