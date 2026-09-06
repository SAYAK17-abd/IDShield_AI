@echo off
echo ==========================================================
echo   IDShield AI - React Web & Mobile Responsive Frontend
echo ==========================================================
echo Starting IDShield AI React Frontend...
echo If backend is running, opening: http://localhost:8080/
echo Opening standalone file directly...
start "" "http://localhost:8080/" 2>nul || start "" "frontend\standalone.html"
start "" "frontend\standalone.html"

