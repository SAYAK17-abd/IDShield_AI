@echo off
echo ===================================================================
echo   IDShield AI - Unified System Startup (Port 8000 + Port 8080)
echo ===================================================================

echo [1/3] Launching Python FastAPI AI Microservice (Port 8000)...
start "IDShield AI Microservice (Port 8000)" cmd /k "call run_ai_service.bat"

echo [2/3] Waiting 3 seconds for AI models to warm up...
timeout /t 3 /nobreak >nul

echo [3/3] Launching Spring Boot Security Gateway (Port 8080)...
start "IDShield Backend Gateway (Port 8080)" cmd /k "call run_backend.bat"

echo.
echo ===================================================================
echo   System services launched!
echo   - AI Microservice: http://127.0.0.1:8000/health
echo   - Spring Gateway:  http://127.0.0.1:8080/actuator/health
echo   - Frontend:        http://127.0.0.1:8080/
echo ===================================================================
timeout /t 5 >nul
start "" "http://localhost:8080/"
