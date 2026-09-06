# IDShield AI - Desktop Client Launcher (PowerShell)
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "$env:USERPROFILE\.jdks\temurin-21" }
$javaExe = "$javaHome\bin\java.exe"

if (-not (Test-Path $javaExe)) {
    $javaExe = "java"
}

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - Desktop Verification Client" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Connecting to Spring Boot Security Gateway (http://127.0.0.1:8080)" -ForegroundColor Yellow
Write-Host "Using Java: $javaExe" -ForegroundColor Gray

& $javaExe src\main\java\com\project\client\IDShieldApp.java
