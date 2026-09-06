@echo off
echo ==========================================================
echo   IDShield AI - Spring Boot Security Gateway (Port 8080)
echo ==========================================================

set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-21"
if not exist "%JAVA_HOME%\bin\java.exe" (
    where java >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Java 21 runtime not found!
        pause
        exit /b 1
    )
    set "JAVA_CMD=java"
) else (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

if exist "target\idshield-backend-1.0.0-SNAPSHOT.jar" (
    echo Starting from pre-built JAR package...
    "%JAVA_CMD%" -jar target\idshield-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
) else (
    echo Pre-built JAR not found. Launching via Maven...
    call "%USERPROFILE%\.maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
)

pause
