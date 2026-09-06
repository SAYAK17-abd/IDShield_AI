# Launch Spring Boot Backend Server
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - Spring Boot Security Gateway (Port 8080)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$javaHome = "$env:USERPROFILE\.jdks\temurin-21"
if (Test-Path "$javaHome\bin\java.exe") {
    $env:JAVA_HOME = $javaHome
    $javaBin = "$javaHome\bin\java.exe"
} else {
    $javaBin = "java"
}

if (Test-Path "target\idshield-backend-1.0.0-SNAPSHOT.jar") {
    Write-Host "Starting pre-built JAR package on port 8080..." -ForegroundColor Green
    & $javaBin -jar target\idshield-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
} else {
    Write-Host "Starting via Maven spring-boot:run..." -ForegroundColor Yellow
    & "$env:USERPROFILE\.maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
}
