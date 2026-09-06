# IDShield AI — React Frontend (Mobile & PC)

A responsive **React Single-Page Application (SPA)** for **SIH26188 — AI-Based Fake Identity & Document Screening System**.

---

## Features

- **📱 Mobile & 💻 PC Cross-Compatible**:
  - Desktop: Dual-column forensic command center with drag-and-drop file ingestion, live image previews, and real-time risk gauges.
  - Mobile: Touch-first responsive interface with native camera activation (`capture="environment"` for documents and `capture="user"` for live facial selfies).
- **🇮🇳 Full Indian Document Catalog**:
  - **Aadhaar Card** (UIDAI 12-digit format & QR check)
  - **PAN Card** (Income Tax 10-char alphanumeric `ABCDE1234F`)
  - **Voter ID / EPIC** (Election Commission)
  - **Driving License** (MoRTH / Sarathi)
  - **Indian Passport** (MEA Machine-Readable Zone)
  - **Academic / University Student ID** (College Enrollment)
  - **Vehicle Registration Certificate (RC)** (Vahan)
  - **Ration Card / NFSA Smart Card** (Food & Supplies)
- **👥 Seamless Role Switcher (Admin, Officer, User)**:
  - 🛡️ **System Administrator (`admin@idshield.com`)**: Full access and forensic audit trail inspection.
  - 🔍 **Verification Officer / Investigator (`investigator@idshield.com`)**: Case review queue with 1-click decision actions (`Approve Authentic`, `Flag for Review`, `Reject as Fake`).
  - 👤 **Citizen User (`user@idshield.com`)**: Self-service document screening and personal verification certificates.
- **⚡ Zero-Friction Execution**:
  - Works immediately by double-clicking `run_frontend.bat` or opening `frontend/index.html` in any modern web browser.
  - Also served directly by Spring Boot at `http://localhost:8080/`.

---

## How to Run

### Option 1: Instant Browser (Recommended, Zero Install)
Double-click `run_frontend.bat` or open `frontend/index.html` in Chrome/Edge.

### Option 2: Mobile Browser Access (Same Wi-Fi)
1. Ensure your PC and phone are on the same Wi-Fi network.
2. Find your PC's IP address (e.g. `ipconfig` $\rightarrow$ `192.168.1.5`).
3. Open `http://192.168.1.5:8080/` in your phone's browser (Safari, Chrome).
4. Tap "Use Smartphone Camera" to snap photos of ID cards and test verification live!

### Option 3: Standard Vite Development Server (with Node.js)
```bash
cd frontend
npm install
npm run dev
```

### Option 4: Packaging as an Android / iOS Mobile App (Capacitor)
To bundle into a native Android APK:
```bash
cd frontend
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init IDShield com.idshield.app
npx cap add android
npx cap open android
```
