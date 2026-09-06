# IDShield AI - Fast Python Computer Vision & Biometrics Microservice (SIH26188)

Production-grade real computer vision and document forensic pipeline communicating with Spring Boot Gateway.

## Capabilities

1. **InsightFace ArcFace Biometrics (`buffalo_sc`)**:
   - 512-dimensional face feature vectors.
   - Genuine Cosine Similarity ($\tau = 0.45$).
   - Explicit edge-case handling: `FACE_NOT_FOUND_DOCUMENT`, `FACE_NOT_FOUND_SELFIE`, `MULTIPLE_FACES_DETECTED`.
2. **RapidOCR (PaddleOCR ONNX Engine)**:
   - High-throughput OCR text extraction.
   - Structured Indian document field regex parser (Aadhaar, PAN, Voter ID, Driving Licence, Student ID).
3. **Forensic Document Tampering Analysis**:
   - Error Level Analysis (ELA) for localized JPEG recompression anomalies.
   - 16-tile spatial noise variance consistency inspection.
4. **Objective Image Quality Checks**:
   - Laplacian variance blur detection.
   - Luminance and contrast checks.
   - Resolution compliance.

## API Endpoints

- `GET /health` -> `{"status": "UP", "modelsLoaded": true}`
- `POST /ai/analyze` -> Full biometric, tampering, OCR, and explainability payload.
