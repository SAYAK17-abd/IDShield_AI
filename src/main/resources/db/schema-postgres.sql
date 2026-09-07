-- ==============================================================================
-- SIH26188 — AI-Based Fake Identity & Document Screening System (IDShield AI)
-- POSTGRESQL PRODUCTION DATABASE SCHEMA DDL
-- ==============================================================================

-- Create Database (Run independently if connecting to a specific database directly)
-- CREATE DATABASE idshield_db;

-- Connect to target database before running the table DDL below:
-- \c idshield_db;

-- ==============================================================================
-- 1. USERS & ACCESS CONTROL
-- ==============================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER'
        CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_INVESTIGATOR')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ==============================================================================
-- 2. CRYPTOGRAPHIC REFRESH TOKENS (Family Rotation & Reuse Protection)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_hash VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);

-- ==============================================================================
-- 3. IDENTITY DOCUMENTS (Secure File Metadata & Off-DB Storage References)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(100) NOT NULL UNIQUE,
    storage_path VARCHAR(500) NOT NULL,
    selfie_storage_path VARCHAR(500),
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'VERIFIED', 'REJECTED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documents_owner ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);

-- ==============================================================================
-- 4. VERIFICATION RESULTS (AI Forensics, Biometrics, OCR & Investigator Actions)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS verification_results (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL UNIQUE,
    investigation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (investigation_status IN ('PENDING', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED')),
    risk_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    tampering_detected BOOLEAN NOT NULL DEFAULT FALSE,
    tampering_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    face_matched BOOLEAN NOT NULL DEFAULT FALSE,
    face_match_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ocr_data_json TEXT,
    inconsistencies_json TEXT,
    reasons_json TEXT,
    reviewed_by_user_id BIGINT,
    investigator_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_verification_reviewer
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_verification_document ON verification_results(document_id);
CREATE INDEX IF NOT EXISTS idx_verification_status ON verification_results(investigation_status);
CREATE INDEX IF NOT EXISTS idx_verification_risk_level ON verification_results(risk_level);

-- ==============================================================================
-- 5. AUDIT LOGS (Append-Only Security & Forensic Event Journal)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    user_email VARCHAR(150),
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    ip_address VARCHAR(50),
    details VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at);

-- ==============================================================================
-- 6. DEFAULT SEED DATA (Admin Account)
-- Default Password: AdminPassword123!
-- BCrypt Hash: $2a$10$wE99N95r0VjZ6rJm7Ciq4OKt/y/D29G7e79y39g5V4p7J4dKqL5Ua
-- ==============================================================================
INSERT INTO users (name, email, password_hash, role, enabled)
VALUES (
    'System Administrator',
    'admin@example.com',
    '$2a$10$wE99N95r0VjZ6rJm7Ciq4OKt/y/D29G7e79y39g5V4p7J4dKqL5Ua',
    'ROLE_ADMIN',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

