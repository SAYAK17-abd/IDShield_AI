@echo off
echo ==========================================================
echo   IDShield AI - Python FastAPI AI Microservice (Port 8000)
echo ==========================================================
cd /d "%~dp0ai-service"

where python >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Python not found on PATH! Please install Python 3.10+.
    pause
    exit /b 1
)

echo Starting FastAPI AI Service on http://127.0.0.1:8000 ...
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload

pause
