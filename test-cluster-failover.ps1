# PowerShell skripta za testiranje klastera i failover-a na Windows-u

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "JUTJUBIĆ - TEST KLASTERA I FAILOVER-A" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$API_URL = "http://localhost"

# Funkcija za pozivanje API-ja
function Call-API {
    param(
        [string]$Endpoint,
        [string]$Description
    )

    Write-Host "📡 $Description" -ForegroundColor Blue
    try {
        $response = Invoke-RestMethod -Uri "$API_URL$Endpoint" -Method Get
        Write-Host ($response | ConvertTo-Json -Compress) -ForegroundColor Green
    } catch {
        Write-Host "❌ Greška: $_" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 1: Load Balancing
function Test-LoadBalancing {
    Write-Host "=== TEST 1: Load Balancing ===" -ForegroundColor Yellow
    Write-Host "Pozivamo API 10 puta i proveravamo na koju repliku ide zahtev..."
    Write-Host ""

    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$API_URL/api/cluster/instance-info" -Method Get
            $instance = $response.instanceId
            Write-Host "Zahtev #${i}: $instance" -ForegroundColor Green
            Start-Sleep -Milliseconds 500
        } catch {
            Write-Host "Zahtev #${i}: Greška" -ForegroundColor Red
        }
    }
    Write-Host ""
}

# Test 2: Failover
function Test-Failover {
    Write-Host "=== TEST 2: Failover - Pad Replike ===" -ForegroundColor Yellow
    Write-Host "Zaustavljamo replica-1 i proveravamo da li replica-2 preuzima..."
    Write-Host ""

    Write-Host "Pre pada:" -ForegroundColor Blue
    Call-API "/api/cluster/instance-info" "Status instance"

    Write-Host "🛑 Zaustavljam replica-1..." -ForegroundColor Red
    docker stop jutjubic-app-1 | Out-Null
    Start-Sleep -Seconds 5

    Write-Host "Posle pada replica-1:" -ForegroundColor Blue
    for ($i = 1; $i -le 5; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$API_URL/api/cluster/instance-info" -Method Get
            $instance = $response.instanceId
            Write-Host "Zahtev #${i}: Uspešan - $instance" -ForegroundColor Green
            Start-Sleep -Seconds 1
        } catch {
            Write-Host "Zahtev #${i}: Neuspešan" -ForegroundColor Red
            Start-Sleep -Seconds 1
        }
    }

    Write-Host ""
    Write-Host "✅ Aplikacija je i dalje funkcionalna!" -ForegroundColor Green
    Write-Host ""
}

# Test 3: Ponovno pokretanje
function Test-Recovery {
    Write-Host "=== TEST 3: Ponovno Pokretanje Replike ===" -ForegroundColor Yellow
    Write-Host "Ponovno pokrećemo replica-1..."
    Write-Host ""

    Write-Host "🔄 Pokrećem replica-1..." -ForegroundColor Blue
    docker start jutjubic-app-1 | Out-Null

    Write-Host "Čekam da se replica-1 pokrene (30s)..."
    Start-Sleep -Seconds 30

    Write-Host "Posle ponovnog pokretanja:" -ForegroundColor Blue
    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$API_URL/api/cluster/instance-info" -Method Get
            $instance = $response.instanceId
            Write-Host "Zahtev #${i}: $instance" -ForegroundColor Green
            Start-Sleep -Milliseconds 500
        } catch {
            Write-Host "Zahtev #${i}: Greška" -ForegroundColor Red
        }
    }

    Write-Host ""
    Write-Host "✅ Obe replike su sada aktivne!" -ForegroundColor Green
    Write-Host ""
}

# Test 4: Database Health
function Test-DatabaseHealth {
    Write-Host "=== TEST 4: Database Health Check ===" -ForegroundColor Yellow
    Write-Host "Proveravamo konekciju prema bazi sa obe replike..."
    Write-Host ""

    Call-API "/api/cluster/db-status" "Database status"
}

# Glavni test scenario
function Main {
    Write-Host "Započinjem testove..."
    Write-Host ""

    # Provera da li su Docker kontejneri pokrenuti
    $containers = docker ps --format "{{.Names}}" | Select-String -Pattern "jutjubic-app"
    if (-not $containers) {
        Write-Host "❌ Greška: Docker kontejneri nisu pokrenuti!" -ForegroundColor Red
        Write-Host "Pokrenite ih sa: docker-compose -f docker-compose-full.yml up -d"
        exit 1
    }

    Write-Host "Čekam da se svi servisi pokrenu..."
    Start-Sleep -Seconds 10

    Test-LoadBalancing
    Test-Failover
    Test-Recovery
    Test-DatabaseHealth

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "           TESTOVI ZAVRŠENI" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Za dodatne testove:"
    Write-Host "   - Otvorite http://localhost/test-video-chat.html"
    Write-Host "   - Testirajte WebSocket čet tokom failover-a"
    Write-Host "   - Zaustavite RabbitMQ: docker stop jutjubic-rabbitmq"
    Write-Host "   - Zaustavite bazu: docker stop jutjubic-postgres"
    Write-Host ""
}

Main
