# Launch React Frontend in Default Web Browser
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - React Web & Mobile Responsive Frontend" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
$backendOnline = $false
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 1 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($resp.StatusCode -eq 200) { $backendOnline = $true }
} catch {}

if ($backendOnline) {
    Write-Host "Spring Boot Backend is ONLINE! Opening http://localhost:8080/ ..." -ForegroundColor Green
    Start-Process "http://localhost:8080/"
} else {
    Write-Host "Spring Boot Backend is offline or starting. Opening standalone.html..." -ForegroundColor Yellow
    Write-Host "Tip: Run the backend with: mvn spring-boot:run" -ForegroundColor Gray
    Start-Process "frontend\standalone.html"
}

