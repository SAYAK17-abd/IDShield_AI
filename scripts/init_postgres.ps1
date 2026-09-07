# ==============================================================================
# SIH26188 — AI-Based Fake Identity & Document Screening System (IDShield AI)
# Automated PostgreSQL Database Initialization Script
# ==============================================================================

param(
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbUser = "postgres",
    [string]$DbPassword = "",
    [string]$DbName = "idshield_db"
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  IDShield AI - PostgreSQL Database Initialization" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Locate psql.exe
$PsqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $PsqlPath)) {
    $psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlCmd) {
        $PsqlPath = $psqlCmd.Source
    } else {
        Write-Error "Could not find psql.exe. Please install PostgreSQL or add its bin directory to PATH."
        exit 1
    }
}
Write-Host "[+] Found psql at: $PsqlPath" -ForegroundColor Green

# 2. Get Password if not provided
if (-not $DbPassword) {
    if ($env:PGPASSWORD) {
        $DbPassword = $env:PGPASSWORD
    } else {
        $DbPassword = Read-Host "Enter PostgreSQL password for user '$DbUser'" -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($DbPassword)
        $DbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    }
}

$env:PGPASSWORD = $DbPassword

# 3. Check connection to postgres server
Write-Host "[*] Testing connection to PostgreSQL at ${DbHost}:${DbPort}..." -ForegroundColor Yellow
& $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d postgres -c "SELECT 1;" > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[-] Authentication failed for user '$DbUser'." -ForegroundColor Red
    Write-Host "[-] Please enter the actual password set during your PostgreSQL installation." -ForegroundColor Yellow
    exit 1
}
Write-Host "[+] Connection successful!" -ForegroundColor Green

# 4. Create database if it does not exist
Write-Host "[*] Checking if database '$DbName' exists..." -ForegroundColor Yellow
$dbExists = & $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname='$DbName';"
if (-not ($dbExists -match "1")) {
    Write-Host "[*] Creating database '$DbName'..." -ForegroundColor Yellow
    & $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d postgres -c "CREATE DATABASE $DbName;"
    Write-Host "[+] Database '$DbName' created successfully." -ForegroundColor Green
} else {
    Write-Host "[+] Database '$DbName' already exists." -ForegroundColor Green
}

# 5. Apply Schema DDL
$SchemaFile = Join-Path $PSScriptRoot "..\src\main\resources\db\schema-postgres.sql"
if (-not (Test-Path $SchemaFile)) {
    $SchemaFile = "src\main\resources\db\schema-postgres.sql"
}

if (-not (Test-Path $SchemaFile)) {
    Write-Error "Cannot locate schema file: $SchemaFile"
    exit 1
}

Write-Host "[*] Applying schema DDL from: $SchemaFile..." -ForegroundColor Yellow
& $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName -f $SchemaFile
if ($LASTEXITCODE -eq 0) {
    Write-Host "[+] Schema successfully applied to '$DbName'!" -ForegroundColor Green
} else {
    Write-Error "Failed to apply schema."
    exit 1
}

# 6. Verify Tables Created
Write-Host "`n[+] Created Tables in '$DbName':" -ForegroundColor Cyan
& $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;"

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  PostgreSQL Initialization Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

