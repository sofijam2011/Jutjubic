#!/bin/bash


echo "🚀 Load Test Script - Jutjubic Application"
echo "==========================================="
echo ""

GREEN='\33[0;32m'
YELLOW='\33[1;33m'
RED='\33[0;31m'
NC='\33[0m'

echo "📡 Proveravamo da li aplikacija radi..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo -e "${RED}❌ Aplikacija ne radi na http://localhost:8081${NC}"
    echo "Pokreni aplikaciju prvo!"
    exit 1
fi
echo -e "${GREEN}✅ Aplikacija radi${NC}"
echo ""

echo "📊 Proveravamo da li Prometheus radi..."
if ! curl -s http://localhost:9090/-/ready > /dev/null; then
    echo -e "${YELLOW}⚠️  Prometheus ne radi na http://localhost:9090${NC}"
    echo "Pokreni Prometheus: brew services start prometheus"
else
    echo -e "${GREEN}✅ Prometheus radi${NC}"
fi
echo ""

create_test_user() {
    local username=$1
    local email="${username}@test.com"
    local password="Test1234!"

    curl -s -X POST http://localhost:8081/api/auth/register \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"email\":\"$email\",\"password\":\"$password\"}" \
        > /dev/null 2>&1
}

login_user() {
    local username=$1
    local password="Test1234!"

    local response=$(curl -s -X POST http://localhost:8081/api/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    echo "$response" | grep -o '"token":"[^"]*' | sed 's/"token":"//'
}

echo "👥 Kreiramo test korisnike..."
for i in {1..20}; do
    create_test_user "loadtest_user$i"
done
echo -e "${GREEN}✅ Kreirano 20 test korisnika${NC}"
echo ""

echo "🔐 Logovanje korisnika i dobijanje JWT tokena..."
declare -a TOKENS
for i in {1..20}; do
    token=$(login_user "loadtest_user$i")
    if [ -n "$token" ]; then
        TOKENS[$i]=$token
        echo -e "${GREEN}✓${NC} User $i logged in"
    else
        echo -e "${RED}✗${NC} User $i login failed"
    fi
done
echo ""

echo "📊 Početne metrike:"
echo "-------------------"
curl -s http://localhost:8081/actuator/prometheus | grep -E "active_users_count|hikaricp_connections_active|hikaricp_connections_idle|system_cpu_usage" | grep -v "#"
echo ""

run_load_test() {
    local duration=$1
    local requests_per_second=$2
    local total_requests=$((duration * requests_per_second))
    local delay=$(echo "scale=4; 1/$requests_per_second" | bc)

    echo -e "${YELLOW}🔥 Pokrećemo load test...${NC}"
    echo "   Trajanje: ${duration}s"
    echo "   Zahteva po sekundi: ${requests_per_second}"
    echo "   Ukupno zahteva: ${total_requests}"
    echo "   Delay između zahteva: ${delay}s"
    echo ""

    local start_time=$(date +%s)
    local request_count=0

    while [ $request_count -lt $total_requests ]; do
        local user_index=$((RANDOM % 20 + 1))
        local token=${TOKENS[$user_index]}

        if [ -n "$token" ]; then
            local endpoint_choice=$((RANDOM % 4))

            case $endpoint_choice in
                0)
                    curl -s -X GET http://localhost:8081/api/videos \
                        -H "Authorization: Bearer $token" > /dev/null &
                    ;;
                1)
                    curl -s -X GET http://localhost:8081/api/users/profile \
                        -H "Authorization: Bearer $token" > /dev/null &
                    ;;
                2)
                    curl -s http://localhost:8081/actuator/health > /dev/null &
                    ;;
                3)
                    curl -s -X GET http://localhost:8081/api/videos/popular \
                        -H "Authorization: Bearer $token" > /dev/null &
                    ;;
            esac
        fi

        request_count=$((request_count + 1))

        if [ $((request_count % 50)) -eq 0 ]; then
            local current_time=$(date +%s)
            local elapsed=$((current_time - start_time))
            local actual_rps=$((request_count / elapsed))
            echo -e "${GREEN}   ➤ Poslato: $request_count/$total_requests zahteva (${actual_rps} req/s)${NC}"
        fi

        sleep $delay
    done

    wait

    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    local actual_rps=$((total_requests / total_duration))

    echo ""
    echo -e "${GREEN}✅ Load test završen!${NC}"
    echo "   Ukupno trajanje: ${total_duration}s"
    echo "   Prosečno zahteva/s: ${actual_rps}"
    echo ""
}

echo "🔥 FAZA 1: Umereno opterećenje (50 req/s za 10s)"
run_load_test 10 50

sleep 3

echo "🔥 FAZA 2: Veliko opterećenje (200 req/s za 15s)"
run_load_test 15 200

sleep 3

echo "🔥 FAZA 3: Ekstremno opterećenje (300 req/s za 10s)"
run_load_test 10 300

echo ""
echo "📊 Završne metrike:"
echo "-------------------"
curl -s http://localhost:8081/actuator/prometheus | grep -E "active_users_count|hikaricp_connections_active|hikaricp_connections_idle|system_cpu_usage" | grep -v "#"
echo ""

echo "📈 HTTP Zahtevi (poslednji minut):"
echo "-----------------------------------"
curl -s http://localhost:8081/actuator/prometheus | grep "http_server_requests_seconds_count" | grep -v "#" | head -10
echo ""

echo -e "${GREEN}✅ Test završen!${NC}"
echo ""
echo "🌐 Proveri metrike na:"
echo "   • Grafana:    http://localhost:3000"
echo "   • Prometheus: http://localhost:9090"
echo "   • Metrics:    http://localhost:8081/actuator/prometheus"
echo ""
echo "📊 Prometheus Query primeri:"
echo "   • active_users_count"
echo "   • hikaricp_connections_active"
echo "   • hikaricp_connections_idle"
echo "   • rate(http_server_requests_seconds_count[1m])"
echo "   • system_cpu_usage * 100"
echo ""
