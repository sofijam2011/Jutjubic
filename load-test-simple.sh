#!/bin/bash


baseUrl="http://localhost:8081"
durationSeconds=10
requestsPerSecond=200

echo ""
echo "======================================"
echo "  LOAD TEST - DATABASE CONNECTIONS"
echo "======================================"
echo ""
echo "Konfiguracija:"
echo "  URL: $baseUrl"
echo "  Target: $requestsPerSecond requests/second"
echo "  Trajanje: $durationSeconds sekundi"
echo "  Ukupno: $((requestsPerSecond * durationSeconds)) zahteva"
echo ""

if ! curl -s -f --max-time 5 "$baseUrl/actuator/health" > /dev/null 2>&1; then
    echo "ERROR: Aplikacija nije dostupna!"
    echo "Pokreni backend: mvn spring-boot:run"
    exit 1
fi

echo "Aplikacija je ONLINE!"
echo ""
echo "Otvori Grafana dashboard: http://localhost:3001/d/jutjubic-dashboard"
echo ""
echo "Pocinje test za 2 sekunde..."
sleep 2

echo ""
echo "POKRENUTO! Saljem zahteve..."
echo ""

endpoints=(
    "/api/videos"
    "/api/videos"
    "/api/videos"
    "/api/users/1"
    "/api/users/2"
)

totalRequests=0
successCount=0
errorCount=0
startTime=$(date +%s)
endTime=$((startTime + durationSeconds))

send_request() {
    local endpoint=$1
    local id=$2

    if curl -s -f --max-time 60 "$baseUrl$endpoint" > /dev/null 2>&1; then
        echo "ok" > /tmp/loadtest_$$_$id
    else
        echo "err" > /tmp/loadtest_$$_$id
    fi
}

secondCounter=0
batchSize=10
batchesPerSecond=20
delayBetweenBatches=0.05

while [ $(date +%s) -lt $endTime ]; do
    secondStartTime=$(python3 -c 'import time; print(time.time())')

    for ((batch=0; batch<batchesPerSecond; batch++)); do
        batchStartTime=$(python3 -c 'import time; print(time.time())')

        for ((i=0; i<batchSize; i++)); do
            endpoint=${endpoints[$RANDOM % ${#endpoints[@]}]}
            reqId="${secondCounter}_${batch}_${i}"
            send_request "$endpoint" "$reqId" &
        done

        totalRequests=$((totalRequests + batchSize))

        if [ $batch -lt $((batchesPerSecond - 1)) ]; then
            batchElapsed=$(python3 -c "import time; print(time.time() - $batchStartTime)")
            sleepTime=$(python3 -c "print(max(0, $delayBetweenBatches - $batchElapsed))")
            if [ $(echo "$sleepTime > 0" | bc -l) -eq 1 ]; then
                sleep $sleepTime
            fi
        fi
    done

    elapsed=$(($(date +%s) - startTime))
    if [ $elapsed -gt 0 ]; then
        currentRPS=$((totalRequests / elapsed))
        printf "\rZahtevi: %d | RPS: %d | Pending...   " $totalRequests $currentRPS
    fi

    secondElapsed=$(python3 -c "import time; print(time.time() - $secondStartTime)")
    sleepTime=$(python3 -c "print(max(0, 1.0 - $secondElapsed))")
    if [ $(echo "$sleepTime > 0" | bc -l) -eq 1 ]; then
        sleep $sleepTime
    fi

    secondCounter=$((secondCounter + 1))
done

echo ""
echo "Cekam da se svi zahtevi zavrse..."

wait

batchSize=10
batchesPerSecond=20
for ((sec=0; sec<secondCounter; sec++)); do
    for ((batch=0; batch<batchesPerSecond; batch++)); do
        for ((i=0; i<batchSize; i++)); do
            tmpFile="/tmp/loadtest_$$_${sec}_${batch}_${i}"
            if [ -f "$tmpFile" ]; then
                result=$(cat "$tmpFile")
                if [ "$result" = "ok" ]; then
                    successCount=$((successCount + 1))
                else
                    errorCount=$((errorCount + 1))
                fi
                rm -f "$tmpFile"
            fi
        done
    done
done

echo ""
totalElapsed=$(($(date +%s) - startTime))
if [ $totalElapsed -gt 0 ]; then
    avgRPS=$((totalRequests / totalElapsed))
else
    avgRPS=0
fi

echo "======================================"
echo "  TEST ZAVRSEN"
echo "======================================"
echo ""
echo "Statistika:"
echo "  Trajanje: ${totalElapsed}s"
echo "  Ukupno zahteva: $totalRequests"
echo "  Uspesni: $successCount"
echo "  Neuspesni: $errorCount"
if [ $totalRequests -gt 0 ]; then
    successRate=$((successCount * 100 / totalRequests))
    echo "  Success rate: ${successRate}%"
fi
echo "  Prosecno RPS: $avgRPS"
echo ""
echo "Proveri Grafana dashboard:"
echo "  http://localhost:3001/d/jutjubic-dashboard"
echo ""
echo "Trebalo bi da vidis:"
echo "  - Active Connections je skocilo"
echo "  - Idle Connections je palo"
echo ""

rm -f /tmp/loadtest_$$_*
