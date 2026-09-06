# Launch React Frontend in Default Web Browser
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - React Web & Mobile Responsive Frontend" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Opening frontend in your browser..." -ForegroundColor Green
Write-Host "Connecting to Spring Boot Security Gateway (http://localhost:8080)" -ForegroundColor Gray

Start-Process "frontend\standalone.html"

