# SIH26188 — AI-Based Fake Identity & Document Screening System
## Central Backend API Gateway & Security Layer

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-blue.svg)](https://spring.io/projects/spring-security)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger%203-green.svg)](https://swagger.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

---

## 1. System Architecture

The Spring Boot backend serves as the **central gateway and security boundary** for the SIH26188 screening system. The frontend dashboard never communicates directly with PostgreSQL or the internal FastAPI AI service.

```
+-------------------------------------------------------------+
|                      React Frontend                         |
+-------------------------------------------------------------+
                              |
                              | HTTPS + JWT Bearer
                              v
+-------------------------------------------------------------+
|               Spring Boot 3.3.x Security Gateway            |
|  - Rate Limiting Filter (Token Bucket: 10/min auth, 60/min) |
|  - Security Headers (CSP, HSTS, X-Content-Type, Frames)    |
|  - JwtAuthenticationFilter (HMAC-SHA256 signature check)   |
|  - Global Exception Handler (@RestControllerAdvice)         |
+-------------------------------------------------------------+
       |                  |                    |
       v                  v                    v
+---------------+  +---------------+  +---------------------+
| Auth Service  |  | Doc Service   |  | Verification Service|
| - BCrypt (12) |  | - Magic Bytes |  | - Risk Engine (30/  |
| - Access/Ref  |  | - UUID store  |  |   30/20/20 weights) |
|   Rotation    |  | - IDOR check  |  | - Status Lifecycle  |
+---------------+  +---------------+  +---------------------+
       |                  |                    |
       |                  |                    v
       |                  |           +---------------------+
       |                  |           | AI Client (FastAPI) |
       |                  |           | - RestClient / Http |
       |                  |           | - Timeouts & Retry  |
       |                  |           | - Mock Fallback     |
       |                  |           +---------------------+
       v                  v                    |
+------------------------------------+         |
|   Spring Data JPA / Hibernate      |         |
|   (PostgreSQL Relational Storage)  |<--------+
|   - Users, RefreshTokens, Docs,    |
|     Verifications, AuditLogs       |
+------------------------------------+
```

---

## 2. Security Threat Model & Mitigations

| # | Security Threat | Backend Mitigation |
|---|---|---|
| 1 | **Account Takeover** | Strong BCrypt password hashing (work factor 12) + complexity validation. |
| 2 | **Brute-Force Login** | Rate-limiting filter (10 requests/min for auth) + generic authentication failure message without username enumeration. |
| 3 | **JWT Theft** | Short-lived access tokens (15 mins) + strict HMAC-SHA256 signing; no sensitive PII in claims. |
| 4 | **Refresh Token Reuse** | Cryptographic SHA-256 token hashing; reuse detection triggers family revocation of all user tokens. |
| 5 | **Privilege Escalation** | Registration strictly defaults to `ROLE_USER`. Role upgrades require `ROLE_ADMIN` via `/api/users/{id}/role`. |
| 6 | **IDOR (Insecure Direct Object Reference)** | SpEL declarative checks (`@documentSecurity.canAccessDocument`) verifying resource ownership before access. |
| 7 | **Malicious File Upload** | Extension whitelist (`.pdf`, `.jpg`, `.jpeg`, `.png`), 10MB limit, and deep **Magic Bytes signature verification** (`%PDF-`, `\xFF\xD8\xFF`, `\x89PNG`). |
| 8 | **Path Traversal** | Client filenames are discarded; server generates UUIDv4 filenames stored outside webroot. |
| 9 | **SQL Injection** | Exclusively parameterized Spring Data JPA/Hibernate queries and Criteria API. |
| 10 | **Stored XSS** | Clean JSON typing with Spring jackson sanitization and CSP/nosniff headers. |
| 11 | **AI Service Abuse** | FastAPI service is unexposed to frontend; called exclusively by authenticated backend client. |
| 12 | **DoS via Heavy Files/Inference** | Multipart size limit (10MB), client connection timeouts (10s connect, 30s read). |
| 13 | **Sensitive Data Leakage** | `@RestControllerAdvice` masks internal stack traces and database errors. Password hashes/tokens excluded from DTOs. |
| 14 | **Insecure CORS** | Configurable `CORS_ALLOWED_ORIGINS` via environment variable; wildcard `*` strictly disallowed. |
| 15 | **Secret Leakage** | All secrets and credentials injected via `.env` / environment variables. |
| 16 | **Excessive / PII Logging** | Structured SLF4J logs with automatic password and token pattern redaction. |

---

## 3. Seeded Demo Accounts (Development Profile)

When running with the default `dev` profile, the following accounts are automatically provisioned for demonstration and hackathon testing:

| Email | Password | Role | Permissions |
|---|---|---|---|
| `admin@idshield.com` | `Admin@123456!` | `ROLE_ADMIN` | Full user role management, system audit logs, review cases |
| `investigator@idshield.com` | `Investigator@123456!` | `ROLE_INVESTIGATOR` | View all documents & verifications, update case status |
| `user@idshield.com` | `User@123456!` | `ROLE_USER` | Upload documents, view own documents and verification results |

---

## 4. Transparent Risk Scoring Formula

The backend applies transparent weighted scoring to AI inspection signals:

$$\text{Risk Score} = 0.30 \times \text{Tampering} + 0.30 \times \text{FaceMismatch} + 0.20 \times \text{OCRInconsistency} + 0.20 \times \text{IdentityInconsistency}$$

### Risk Levels:
- **LOW (0–30)**: Minimal anomaly signals detected. Case automatically marked `COMPLETED`.
- **MEDIUM (31–60)**: Moderate discrepancies detected. Case flagged as `REVIEW_REQUIRED`.
- **HIGH (61–100)**: Severe tampering/inconsistency detected. Case flagged as `REVIEW_REQUIRED`.

> **Ethical & Legal Safeguard**: The system identifies **potentially suspicious, inconsistent, or manipulated information** and provides discrete human-readable reasons. It **never** claims that AI definitively proves fraud — the final decision belongs to a human investigator.

---

## 5. API Reference & Swagger UI

Once started, access the interactive OpenAPI/Swagger 3 documentation at:
```
http://localhost:8080/swagger-ui.html
```

### Key Endpoints:
- `POST /api/auth/register` — Register a standard user
- `POST /api/auth/login` — Authenticate and obtain JWT access + refresh tokens
- `POST /api/auth/refresh` — Rotate refresh token and obtain fresh access token
- `POST /api/auth/logout` — Revoke refresh token session
- `GET /api/auth/me` — Inspect current authenticated user
- `POST /api/documents/upload` — Multipart document upload with magic byte verification
- `GET /api/documents/{id}` — IDOR-protected document metadata
- `GET /api/documents/{id}/file` — IDOR-protected document binary download
- `POST /api/verifications/documents/{documentId}` — Trigger AI screening
- `GET /api/verifications/{id}` — Get screening findings and risk breakdown
- `PATCH /api/verifications/{id}/status` — Investigator updates case status
- `GET /api/users` — Admin-only user management
- `GET /api/audit-logs` — Admin-only audit trail
- `GET /actuator/health` — System health check

---

## 6. Running the Backend

### Prerequisites
- Java 25+
- PostgreSQL (or in-memory fallback)
- Docker (optional)

### Option A: Running with Maven
```bash
# Set environment variables (or copy .env.example)
cp .env.example .env

# Run Spring Boot application
mvn spring-boot:run
```

### Option B: Running via Docker
```bash
# Build Docker image
docker build -t idshield-backend:1.0 .

# Run container with environment file
docker run -d -p 8080:8080 --env-file .env.example --name idshield idshield-backend:1.0
```

### Health Check:
```bash
curl http://localhost:8080/actuator/health
```

