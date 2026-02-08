# Load Test - Brzi test sa 200+ zahteva/sec
# Koristi runspace umesto job-ova za bolje performanse

$baseUrl = "http://localhost:8081"
$durationSeconds = 30
$targetRPS = 200

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  LOAD TEST - DATABASE CONNECTIONS" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Konfiguracija:" -ForegroundColor Yellow
Write-Host "  URL: $baseUrl" -ForegroundColor White
Write-Host "  Target: $targetRPS requests/second" -ForegroundColor White
Write-Host "  Trajanje: $durationSeconds sekundi" -ForegroundColor White
Write-Host ""

# Proveri aplikaciju
try {
    Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 5 | Out-Null
    Write-Host "Aplikacija je ONLINE!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Aplikacija nije dostupna!" -ForegroundColor Red
    Write-Host "Pokreni backend: mvn spring-boot:run" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Otvori Grafana dashboard: http://localhost:3001/d/jutjubic-dashboard" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pocinje test za 3 sekunde..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "POKRENUTO! Saljem zahteve..." -ForegroundColor Green
Write-Host ""

# Endpointi koji koriste BAZU i rade bez autentifikacije
$endpoints = @(
    "/api/videos",        # SELECT * FROM videos - radi
    "/api/users/1",       # SELECT * FROM users WHERE id=1 - radi
    "/api/users/2",
    "/api/users/3"
)

# Statistika
$totalRequests = 0
$successCount = 0
$errorCount = 0
$startTime = Get-Date
$endTime = $startTime.AddSeconds($durationSeconds)

# Glavna petlja - salji zahteve u batch-evima
while ((Get-Date) -lt $endTime) {
    $batchStartTime = Get-Date
    
    # Posalji batch od 20 zahteva odjednom (vise za veci RPS)
    for ($i = 0; $i -lt 20; $i++) {
        $endpoint = $endpoints | Get-Random
        
        try {
            Invoke-WebRequest -Uri "$baseUrl$endpoint" -Method GET -TimeoutSec 1 -UseBasicParsing -ErrorAction Stop | Out-Null
            $successCount++
        } catch {
            $errorCount++
        }
        $totalRequests++
    }
    
    # Prikazi progress
    $elapsed = ((Get-Date) - $startTime).TotalSeconds
    if ($elapsed -gt 0) {
        $currentRPS = [Math]::Round($totalRequests / $elapsed, 1)
        Write-Host "`rZahtevi: $totalRequests | RPS: $currentRPS | OK: $successCount | ERR: $errorCount   " -NoNewline -ForegroundColor Cyan
    }
    
    # Pauza da postignemo ~200 RPS (batch od 20 svakih 100ms = 200/sec)
    $batchElapsed = ((Get-Date) - $batchStartTime).TotalMilliseconds
    $sleepTime = [Math]::Max(0, 100 - $batchElapsed)
    if ($sleepTime -gt 0) {
        Start-Sleep -Milliseconds $sleepTime
    }
}

# Finalna statistika
Write-Host ""
Write-Host ""
$totalElapsed = ((Get-Date) - $startTime).TotalSeconds
$avgRPS = [Math]::Round($totalRequests / $totalElapsed, 1)

Write-Host "======================================" -ForegroundColor Green
Write-Host "  TEST ZAVRSEN" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "Statistika:" -ForegroundColor Yellow
Write-Host "  Trajanje: $([Math]::Round($totalElapsed, 1))s" -ForegroundColor White
Write-Host "  Ukupno zahteva: $totalRequests" -ForegroundColor White
Write-Host "  Uspesni: $successCount" -ForegroundColor Green
Write-Host "  Neuspesni: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) {"Red"} else {"Green"})
Write-Host "  Prosecno RPS: $avgRPS" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proveri Grafana dashboard:" -ForegroundColor Yellow
Write-Host "  http://localhost:3001/d/jutjubic-dashboard" -ForegroundColor Cyan
Write-Host ""
Write-Host "Trebalo bi da vidis:" -ForegroundColor Yellow
Write-Host "  - Active Connections je skocilo na 1-5" -ForegroundColor White
Write-Host "  - Idle Connections je palo za 1-5" -ForegroundColor White
Write-Host ""
