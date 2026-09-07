# ==============================================================================
# Reset PostgreSQL Password to 'postgres' and Initialize Database
# Requires Administrator privileges (will auto-elevate if run normally)
# ==============================================================================

# Auto-elevate if not running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[*] Requesting Administrator permissions to configure PostgreSQL service..." -ForegroundColor Yellow
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "`"$PSCommandPath`""
    exit
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Resetting PostgreSQL Master Password to: postgres" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$HbaFile = "C:\Program Files\PostgreSQL\18\data\pg_hba.conf"
$PsqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$ServiceName = "postgresql-x64-18"

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 1. Backup original pg_hba.conf if not already backed up
if (-not (Test-Path "$HbaFile.bak")) {
    Copy-Item $HbaFile "$HbaFile.bak"
}

# 2. Temporarily switch authentication to 'trust' for localhost
Write-Host "[1/5] Enabling local trust mode..." -ForegroundColor Yellow
$content = [System.IO.File]::ReadAllText($HbaFile)
$trustedContent = $content -replace 'scram-sha-256', 'trust'
[System.IO.File]::WriteAllText($HbaFile, $trustedContent, $utf8NoBom)

# 3. Start or Restart PostgreSQL service
Write-Host "[2/5] Starting PostgreSQL service..." -ForegroundColor Yellow
Start-Service $ServiceName

# 4. Connect and update password to 'postgres'
Write-Host "[3/5] Updating master password to 'postgres'..." -ForegroundColor Yellow
& $PsqlPath -h localhost -p 5432 -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"

# 5. Restore secure scram-sha-256 authentication
Write-Host "[4/5] Restoring secure authentication..." -ForegroundColor Yellow
$restoredContent = [System.IO.File]::ReadAllText($HbaFile)
$secureContent = $restoredContent -replace 'trust', 'scram-sha-256'
[System.IO.File]::WriteAllText($HbaFile, $secureContent, $utf8NoBom)

# 6. Restart service with new password
Write-Host "[5/5] Finalizing service restart..." -ForegroundColor Yellow
Restart-Service $ServiceName -Force

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  SUCCESS! PostgreSQL password is now set to: postgres" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "`nYou can now enter 'postgres' as the password in SQLTools!" -ForegroundColor Cyan

