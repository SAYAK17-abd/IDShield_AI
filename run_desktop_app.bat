@echo off
setlocal
echo ===================================================
echo   IDShield AI - Desktop Verification Client
echo ===================================================

if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=%USERPROFILE%\.jdks\temurin-21\bin\java.exe"
)

if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

echo Using Java: "%JAVA_EXE%"
echo Launching IDShield Desktop Application...
"%JAVA_EXE%" src\main\java\com\project\client\IDShieldApp.java
pause
