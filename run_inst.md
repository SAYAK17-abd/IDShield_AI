# IDShield AI (SIH26188) — Execution & Manual Run Guide (`run.md`)

This guide contains complete instructions for running the **IDShield AI - Fake Identity & Document Screening System** manually step-by-step or via 1-click launchers.

---

## 1. System Architecture Ports

| Component | Technology | Default URL / Port |
|---|---|---|
| **AI Microservice** | Python 3.11 + FastAPI + InsightFace + RapidOCR | `http://127.0.0.1:8000` |
| **Security Gateway** | Java 21 + Spring Boot 3.3.x + Spring Security 6 | `http://127.0.0.1:8080` |
| **Web Frontend** | React + Tailwind CSS (Responsive Mobile & PC) | `http://localhost:8080/` or `frontend/standalone.html` |
| **Desktop Client** | Java Swing Forensic UI | Native Window (`run_desktop_app.bat`) |

---

## 2. Manual Step-by-Step Execution

Open two separate PowerShell or Command Prompt windows:

### Terminal 1: Start Python AI Microservice (Port 8000)
```powershell
# 1. Navigate to the AI service directory
cd ai-service

# 2. Start Uvicorn ASGI server
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
- **Expected Console Output**:
  ```text
  [INFO] [ai_service.main]: Starting IDShield AI Service (FastAPI)...
  [INFO] [ai_service.models.face]: InsightFace model [buffalo_sc] successfully loaded and ready.
  [INFO] [ai_service.models.ocr]: RapidOCR engine successfully loaded and ready.
  INFO:  Uvicorn running on http://127.0.0.1:8000
  ```
- **Verify Health**: Visit `http://127.0.0.1:8000/health` in browser:
  ```json
  {"status": "UP", "modelsLoaded": true}
  ```

---

### Terminal 2: Start Spring Boot Security Gateway (Port 8080)
From the project root directory (`d:\CODES\project.all\own\AI_projects\IDShild AI_Backend`):

**Option A (Using Pre-Built JAR — Fastest)**:
```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
& "$env:JAVA_HOME\bin\java.exe" -jar target\idshield-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

**Option B (Using Maven)**:
```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
& "$env:USERPROFILE\.maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
```
- **Expected Console Output**:
  ```text
  IDShield Backend Gateway started successfully.
  Swagger UI documentation available at: /swagger-ui.html
  Actuator Health endpoint available at: /actuator/health
  Tomcat started on port 8080 (http)
  ```
- **Verify Gateway Health**: Visit `http://127.0.0.1:8080/actuator/health` in browser:
  ```json
  {"status": "UP"}
  ```

---

### Terminal 3: Access the React Frontend

**Option A (Recommended — Direct via Spring Boot)**:
- Open your browser to **`http://localhost:8080/`**
- Both PC and Mobile phones connected to the same Wi-Fi network can access `http://<YOUR_PC_IP>:8080/`.

**Option B (Zero-Install Standalone File)**:
- Double-click `frontend\standalone.html` in File Explorer.

**Option C (Vite Dev Server)**:
```powershell
cd frontend
npm install
npm run dev
```

---

## 3. 1-Click Launchers (Windows Scripts)

If you do not want to run commands manually in terminals, use the pre-configured root batch scripts:

| Script | Action |
|---|---|
| [`start_all.bat`](start_all.bat) | **1-Click Master Launch**: Starts AI Service (8000), Spring Boot (8080), and opens the web app in your default browser. |
| [`run_ai_service.bat`](run_ai_service.bat) | Starts Python AI service on port 8000 (auto-clears any occupied port conflicts). |
| [`run_backend.bat`](run_backend.bat) | Starts Spring Boot on port 8080 with Java 21 environment. |
| [`run_frontend.bat`](run_frontend.bat) | Launches the React web interface in your default browser. |
| [`run_desktop_app.bat`](run_desktop_app.bat) | Launches the Java Swing Desktop Client. |

---

## 4. Testing the System

### Test Accounts (Pre-Seeded in Dev Profile):
- **Admin**: `admin@idshield.com` / `Admin@123456!`
- **Investigator / Officer**: `investigator@idshield.com` / `Investigator@123456!`
- **Citizen User**: `user@idshield.com` / `User@123456!`

### Live Verification Flow:
1. Open `http://localhost:8080/`
2. Select any Indian Document Format (Aadhaar, PAN, Voter ID, Driving Licence, Student ID).
3. Upload an identity document image or PDF.
4. (Optional) Upload a live reference selfie or capture via webcam/phone camera.
5. Click **"Run Multi-Signal Security Screening"**.
6. Inspect the AI Verification Report:
   - **Genuine matching person**: Reports `MATCH` (similarity $> 0.50$).
   - **Different person photo**: Accurately reports `MISMATCH` (similarity $\approx 0.05$), with `HIGH` risk level and investigation status `REVIEW_REQUIRED`.
   - **Extracted OCR**: Live parsing of names and ID numbers without simulated values.

---

## 5. Running Automated Test Suites

### AI Biometrics & Acceptance Tests:
```powershell
cd ai-service
python tests\test_end_to_end_faces.py
```
- Validates same-person matching, different-person mismatching, and pipeline risk signal synthesis.

### Spring Boot Security & Unit Tests:
```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
& "$env:USERPROFILE\.maven\apache-maven-3.9.9\bin\mvn.cmd" test
```
- Runs 15 unit/security tests (Surefire BUILD SUCCESS).
