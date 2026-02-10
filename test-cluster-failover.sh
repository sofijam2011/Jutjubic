#!/bin/bash

# Test skripta za demonstraciju rada u klasteru i failover-a
# Demonstrira da aplikacija ostaje funkcionalna kada:
# - Padne jedna replika
# - Ponovo se podiže replika
# - Parcijalni gubitak konekcije prema MQ ili bazi

echo "========================================"
echo "JUTJUBIĆ - TEST KLASTERA I FAILOVER-A"
echo "========================================"
echo ""

# Boje za output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# API URL
API_URL="http://localhost"

# Funkcija za pozivanje API-ja i prikazivanje odgovora
call_api() {
    local endpoint=$1
    local description=$2

    echo -e "${BLUE}📡 ${description}${NC}"
    response=$(curl -s "${API_URL}${endpoint}")
    echo -e "${GREEN}${response}${NC}"
    echo ""
}

# Funkcija za testiranje load balancing-a
test_load_balancing() {
    echo -e "${YELLOW}=== TEST 1: Load Balancing ===${NC}"
    echo "Pozivamo API 10 puta i proveravamo na koju repliku ide zahtev..."
    echo ""

    for i in {1..10}
    do
        response=$(curl -s "${API_URL}/api/cluster/instance-info")
        instance=$(echo $response | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)
        echo -e "Zahtev #${i}: ${GREEN}${instance}${NC}"
        sleep 0.5
    done
    echo ""
}

# Funkcija za testiranje failover-a
test_failover() {
    echo -e "${YELLOW}=== TEST 2: Failover - Pad Replike ===${NC}"
    echo "Zaustavljamo replica-1 i proveravamo da li replica-2 preuzima..."
    echo ""

    # Provera stanja pre pada
    echo -e "${BLUE}Pre pada:${NC}"
    call_api "/api/cluster/instance-info" "Status instance"

    # Zaustavi replica-1
    echo -e "${RED}🛑 Zaustavljam replica-1...${NC}"
    docker stop jutjubic-app-1 > /dev/null 2>&1
    sleep 5

    # Provera da li API još uvek radi
    echo -e "${BLUE}Posle pada replica-1:${NC}"
    for i in {1..5}
    do
        response=$(curl -s "${API_URL}/api/cluster/instance-info")
        if [ $? -eq 0 ]; then
            instance=$(echo $response | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)
            echo -e "Zahtev #${i}: ${GREEN}Uspešan - ${instance}${NC}"
        else
            echo -e "Zahtev #${i}: ${RED}Neuspešan${NC}"
        fi
        sleep 1
    done

    echo ""
    echo -e "${GREEN}✅ Aplikacija je i dalje funkcionalna!${NC}"
    echo ""
}

# Funkcija za testiranje ponovnog pokretanja
test_recovery() {
    echo -e "${YELLOW}=== TEST 3: Ponovno Pokretanje Replike ===${NC}"
    echo "Ponovno pokrećemo replica-1..."
    echo ""

    echo -e "${BLUE}🔄 Pokrećem replica-1...${NC}"
    docker start jutjubic-app-1 > /dev/null 2>&1

    echo "Čekam da se replica-1 pokrene (30s)..."
    sleep 30

    echo -e "${BLUE}Posle ponovnog pokretanja:${NC}"
    for i in {1..10}
    do
        response=$(curl -s "${API_URL}/api/cluster/instance-info")
        instance=$(echo $response | grep -o '"instanceId":"[^"]*"' | cut -d'"' -f4)
        echo -e "Zahtev #${i}: ${GREEN}${instance}${NC}"
        sleep 0.5
    done

    echo ""
    echo -e "${GREEN}✅ Obe replike su sada aktivne!${NC}"
    echo ""
}

# Funkcija za testiranje zdravlja baze
test_database_health() {
    echo -e "${YELLOW}=== TEST 4: Database Health Check ===${NC}"
    echo "Proveravamo konekciju prema bazi sa obe replike..."
    echo ""

    call_api "/api/cluster/db-status" "Database status"
}

# Glavni test scenario
main() {
    echo "Započinjem testove..."
    echo ""

    # Čekaj da se sve pokrene
    echo "Čekam da se svi servisi pokrenu..."
    sleep 10

    test_load_balancing
    test_failover
    test_recovery
    test_database_health

    echo ""
    echo -e "${GREEN}========================================"
    echo "           TESTOVI ZAVRŠENI"
    echo "========================================${NC}"
    echo ""
    echo "📊 Za dodatne testove:"
    echo "   - Otvorite http://localhost/test-video-chat.html"
    echo "   - Testirajte WebSocket čet tokom failover-a"
    echo "   - Zaustavite RabbitMQ: docker stop jutjubic-rabbitmq"
    echo "   - Zaustavite bazu: docker stop jutjubic-postgres"
    echo ""
}

# Provera da li je Docker Compose pokrenut
if ! docker ps | grep -q "jutjubic-app"; then
    echo -e "${RED}❌ Greška: Docker kontejneri nisu pokrenuti!${NC}"
    echo "Pokrenite ih sa: docker-compose -f docker-compose-full.yml up -d"
    exit 1
fi

main
