# Launch Python FastAPI AI Service on Port 8000
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - Python FastAPI AI Microservice (Port 8000)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "Freeing occupied port 8000 (PID $($_.OwningProcess))..." -ForegroundColor Yellow
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}

Set-Location "$PSScriptRoot\ai-service"
Write-Host "Starting FastAPI AI Service on http://127.0.0.1:8000 ..." -ForegroundColor Green
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
