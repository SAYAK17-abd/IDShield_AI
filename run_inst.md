# SIH26188 — Backend Testing & Execution Instructions (`run_inst.md`)

This guide provides end-to-end instructions for launching, interacting with, and testing the **SIH26188 — Fake Identity & Document Screening System Backend**.

---

## 1. Prerequisites & Environment Setup

- **Java**: Eclipse Temurin JDK 21 LTS (located at `$env:USERPROFILE\.jdks\temurin-21` or your local JDK 21+ installation).
- **Maven**: Apache Maven 3.9.9 (located at `$env:USERPROFILE\.maven\apache-maven-3.9.9\bin` or your local `mvn`).
- **Database**: 
  - For local development / testing, the `dev` profile uses an automatic in-memory H2 database.
  - For production, configure PostgreSQL in `.env` or `application.yml`.

---

## 2. Launching the Backend Server

Open PowerShell in the project directory (`d:\CODES\project.all\own\AI_projects\IDShild AI_Backend`) and run:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
& "$env:JAVA_HOME\bin\java.exe" -jar target\idshield-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

When you see:
```text
IDShield Backend Gateway started successfully.
Swagger UI documentation available at: /swagger-ui.html
Actuator Health endpoint available at: /actuator/health
```
The server is running at **`http://localhost:8080`**.

---

## 3. Pre-Seeded Demonstration Accounts (Dev Profile)

The application automatically seeds three role-specific accounts on startup:

| Role | Email | Password | Allowed Capabilities |
|---|---|---|---|
| **ADMIN** | `admin@idshield.com` | `Admin@123456!` | Role management, immutable audit logs, full case review |
| **INVESTIGATOR** | `investigator@idshield.com` | `Investigator@123456!` | Review cases, inspect documents, update investigation status |
| **USER** | `user@idshield.com` | `User@123456!` | Upload documents, view own documents and verification results |

---

## 4. Interactive Testing via Swagger UI (Recommended)

1. Open your browser to:
   **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

2. **Authenticate**:
   - Scroll to **`POST /api/auth/login`**.
   - Click **Try it out** and provide credentials:
     ```json
     {
       "email": "user@idshield.com",
       "password": "User@123456!"
     }
     ```
   - Click **Execute** and copy the `accessToken` from the response.

3. **Authorize Sessions**:
   - Click the green **Authorize 🔓** button at the top right of the Swagger page.
   - Paste the token into the value field and click **Authorize**.
   - All subsequent requests will now automatically send the `Authorization: Bearer <token>` header.

4. **Explore Protected Endpoints**:
   - **`GET /api/auth/me`**: Inspect current authenticated profile.
   - **`POST /api/documents/upload`**: Upload an identity document (PDF, PNG, JPEG).
   - **`POST /api/verifications/documents/{documentId}`**: Trigger AI verification & view transparent risk scores.
   - **`PATCH /api/verifications/{id}/status`**: Update case investigation status (as Investigator/Admin).
   - **`GET /api/audit-logs`**: Inspect system audit trail (as Admin).

---

## 5. Automated PowerShell Test Script

You can copy and run the following script in a separate PowerShell window to test the entire lifecycle:

```powershell
# ==============================================================================
# Step 1: Health Check
# ==============================================================================
Write-Host "--- 1. Testing Actuator Health ---" -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
Write-Host "Health Status: $($health.status)" -ForegroundColor Green

# ==============================================================================
# Step 2: Login and Obtain JWT Token
# ==============================================================================
Write-Host "`n--- 2. Logging in as user@idshield.com ---" -ForegroundColor Cyan
$loginPayload = @{
    email = "user@idshield.com"
    password = "User@123456!"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $loginPayload

$token = $authResponse.data.accessToken
$refreshToken = $authResponse.data.refreshToken
$headers = @{ Authorization = "Bearer $token" }
Write-Host "JWT Access Token obtained successfully!" -ForegroundColor Green

# ==============================================================================
# Step 3: Get Current Authenticated Profile
# ==============================================================================
Write-Host "`n--- 3. Fetching /api/auth/me profile ---" -ForegroundColor Cyan
$profile = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -Headers $headers
Write-Host "Authenticated as: $($profile.data.name) ($($profile.data.role))" -ForegroundColor Green

# ==============================================================================
# Step 4: Refresh Token Rotation
# ==============================================================================
Write-Host "`n--- 4. Testing Refresh Token Rotation ---" -ForegroundColor Cyan
$refreshPayload = @{ refreshToken = $refreshToken } | ConvertTo-Json
$rotateResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/refresh" `
    -Method Post `
    -ContentType "application/json" `
    -Body $refreshPayload
Write-Host "Token successfully rotated!" -ForegroundColor Green

# ==============================================================================
# Step 5: Upload Identity Document with Magic Bytes
# ==============================================================================
Write-Host "`n--- 5. Uploading Valid Identity Document ---" -ForegroundColor Cyan
$testPdfPath = "$env:TEMP\passport_sample.pdf"
$pdfBytes = [System.Text.Encoding]::ASCII.GetBytes("%PDF-1.4 Identity Document Sample")
[System.IO.File]::WriteAllBytes($testPdfPath, $pdfBytes)

$uploadForm = @{
    file = Get-Item $testPdfPath
    documentType = "PASSPORT"
}

$uploadResult = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/upload" `
    -Method Post `
    -Headers $headers `
    -Form $uploadForm

$docId = $uploadResult.data.id
Write-Host "Document Uploaded: ID=$docId, Checksum=$($uploadResult.data.sha256Checksum)" -ForegroundColor Green

# ==============================================================================
# Step 6: Trigger AI Verification & Risk Scoring
# ==============================================================================
Write-Host "`n--- 6. Triggering AI Verification ---" -ForegroundColor Cyan
$verifyResult = Invoke-RestMethod -Uri "http://localhost:8080/api/verifications/documents/$docId" `
    -Method Post `
    -Headers $headers

Write-Host "Verification Complete!" -ForegroundColor Green
Write-Host "Risk Score: $($verifyResult.data.riskScore)/100 (Level: $($verifyResult.data.riskLevel))" -ForegroundColor Yellow
Write-Host "Status: $($verifyResult.data.investigationStatus)" -ForegroundColor Yellow
Write-Host "Reasons: $($verifyResult.data.reasons -join ', ')" -ForegroundColor Gray
$verificationId = $verifyResult.data.id

# ==============================================================================
# Step 7: IDOR Protection Verification
# ==============================================================================
Write-Host "`n--- 7. Verifying IDOR Defense (Unauthenticated Access) ---" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/documents/$docId" -Method Get
    Write-Host "FAILED: Endpoint allowed unauthorized access!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: IDOR blocked unauthorized request! Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Green
}

# ==============================================================================
# Step 8: Investigator Review & Status Update
# ==============================================================================
Write-Host "`n--- 8. Investigator Review & Status Update ---" -ForegroundColor Cyan
$invPayload = @{ email = "investigator@idshield.com"; password = "Investigator@123456!" } | ConvertTo-Json
$invAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $invPayload
$invHeaders = @{ Authorization = "Bearer $($invAuth.data.accessToken)" }

$updateStatus = @{
    status = "VERIFIED"
    investigatorNotes = "Manual comparison complete. Identity confirmed authentic."
} | ConvertTo-Json

$updateResult = Invoke-RestMethod -Uri "http://localhost:8080/api/verifications/$verificationId/status" `
    -Method Patch `
    -Headers $invHeaders `
    -ContentType "application/json" `
    -Body $updateStatus

Write-Host "Case Updated by Investigator: Status=$($updateResult.data.investigationStatus)" -ForegroundColor Green

# ==============================================================================
# Step 9: Admin Audit Log Inspection
# ==============================================================================
Write-Host "`n--- 9. Admin Inspecting System Audit Logs ---" -ForegroundColor Cyan
$adminPayload = @{ email = "admin@idshield.com"; password = "Admin@123456!" } | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $adminPayload
$adminHeaders = @{ Authorization = "Bearer $($adminAuth.data.accessToken)" }

$auditLogs = Invoke-RestMethod -Uri "http://localhost:8080/api/audit-logs?size=5" -Method Get -Headers $adminHeaders
Write-Host "Retrieved $($auditLogs.data.content.Count) recent audit events from append-only log." -ForegroundColor Green
```

---

## 6. Running the Automated Test Suite

To re-run the 15 automated security, file validation, risk scoring, and authentication unit tests:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
& "$env:USERPROFILE\.maven\apache-maven-3.9.9\bin\mvn.cmd" test
```

Expected Output:
```text
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 7. Launching the IDShield Desktop GUI Client

For interactive live demonstrations without using a web browser or curl, a dedicated dark-themed Java Swing client is included.

### Starting the Client:
Double-click `run_desktop_app.bat` or run in PowerShell:

```powershell
.\run_desktop_app.ps1
```

### Capabilities:
- **Auto-Connects to Gateway**: Connects directly to `http://127.0.0.1:8080`, performs JWT login as `admin@idshield.com`, and displays status.
- **Switch User**: Supports logging in as any user (`investigator@idshield.com`, `user@idshield.com`).
- **File Upload**: Uploads documents with real-time thumbnail preview and magic-byte inspection.
- **Real-Time Risk Report**: Renders live AI verification results (Authenticity Score, Tamper Confidence, Facial Match Confidence, Extracted OCR data, and security integrity flags).

