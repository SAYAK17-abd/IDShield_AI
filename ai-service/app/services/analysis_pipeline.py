import logging
import time
from typing import Optional, List, Dict, Any
import numpy as np

from app.schemas.requests import AiAnalysisRequest
from app.schemas.responses import (
    AiAnalysisResponse,
    RiskIndicatorDto,
    ProcessingMetaDto
)
from app.utils.image_utils import load_image_from_bytes_or_base64
from app.services.face_verification import verify_faces
from app.services.ocr_service import extract_ocr_from_image
from app.services.image_quality import assess_image_quality
from app.services.tamper_detection import detect_tampering

logger = logging.getLogger("ai_service.pipeline")


def run_full_analysis(
    doc_bytes: Optional[bytes] = None,
    selfie_bytes: Optional[bytes] = None,
    request_dto: Optional[AiAnalysisRequest] = None
) -> AiAnalysisResponse:
    """
    Master pipeline that executes genuine pixel-level inspection:
    1. Decodes document image / renders PDF
    2. Decodes selfie image
    3. Executes InsightFace ArcFace face verification
    4. Executes RapidOCR (PaddleOCR ONNX) text extraction
    5. Evaluates document quality (blur, contrast, brightness)
    6. Evaluates tampering indicators (ELA, noise consistency)
    7. Synthesizes explainable signals and risk indicators
    """
    start_time = time.time()

    # 1. Resolve Document Image
    doc_img = None
    if doc_bytes:
        doc_img = load_image_from_bytes_or_base64(data_bytes=doc_bytes)
    elif request_dto and request_dto.fileBase64:
        doc_img = load_image_from_bytes_or_base64(b64_str=request_dto.fileBase64)

    # 2. Resolve Selfie Image
    selfie_img = None
    if selfie_bytes:
        selfie_img = load_image_from_bytes_or_base64(data_bytes=selfie_bytes)
    elif request_dto and request_dto.selfieBase64:
        selfie_img = load_image_from_bytes_or_base64(b64_str=request_dto.selfieBase64)

    doc_type = request_dto.documentType if request_dto else None

    # 3. Face Verification
    face_res = verify_faces(doc_img, selfie_img)

    # 4. OCR Extraction
    ocr_res = extract_ocr_from_image(doc_img, doc_type)

    # 5. Image Quality Assessment
    quality_res = assess_image_quality(doc_img)

    # 6. Tampering Forensics
    tamper_res = detect_tampering(doc_img)

    # 7. Synthesize Inconsistencies & Risk Indicators
    inconsistencies: List[str] = []
    risk_indicators: List[RiskIndicatorDto] = []

    # Face verification signals
    if face_res.status == "MISMATCH":
        inconsistencies.append("Face does not match submitted reference identity")
        risk_indicators.append(RiskIndicatorDto(
            type="FACE_MISMATCH",
            severity="HIGH",
            message=f"Document portrait and selfie failed biometric verification (Cosine Similarity: {face_res.similarity:.2f} < {face_res.threshold:.2f})"
        ))
    elif face_res.status == "FACE_NOT_FOUND_DOCUMENT":
        inconsistencies.append("No face portrait located on identity document")
        risk_indicators.append(RiskIndicatorDto(
            type="FACE_NOT_FOUND_DOCUMENT",
            severity="HIGH",
            message="No valid human portrait detected on the identity document"
        ))
    elif face_res.status == "FACE_NOT_FOUND_SELFIE":
        inconsistencies.append("Selfie image did not contain a detectable human face")
        risk_indicators.append(RiskIndicatorDto(
            type="FACE_NOT_FOUND_SELFIE",
            severity="HIGH",
            message="Biometric reference selfie did not contain a recognizable face"
        ))
    elif face_res.status == "MULTIPLE_FACES_DETECTED":
        inconsistencies.append("Multiple faces detected in document or selfie frame")
        risk_indicators.append(RiskIndicatorDto(
            type="MULTIPLE_FACES_DETECTED",
            severity="HIGH",
            message="Multiple faces detected in capture frame; inspection requires officer review"
        ))
    elif face_res.status == "UNCERTAIN":
        inconsistencies.append("Borderline facial similarity score")
        risk_indicators.append(RiskIndicatorDto(
            type="FACE_UNCERTAIN",
            severity="MEDIUM",
            message=f"Facial similarity is borderline ({face_res.similarity:.2f}); recommended for manual review"
        ))

    # Tampering signals
    if tamper_res.detected:
        for r in tamper_res.reasons:
            inconsistencies.append(r)
        risk_indicators.append(RiskIndicatorDto(
            type="TAMPERING_SUSPECTED",
            severity="HIGH",
            message=f"Forensic inspection detected potential tampering artifacts (Confidence: {tamper_res.confidence:.0%})"
        ))

    # Quality signals
    if quality_res.score < 0.60:
        for iss in quality_res.issues:
            inconsistencies.append(f"Quality Issue: {iss}")
        risk_indicators.append(RiskIndicatorDto(
            type="LOW_IMAGE_QUALITY",
            severity="MEDIUM",
            message="Document image quality is degraded, reducing OCR and biometric accuracy"
        ))

    # OCR text confidence signal
    if ocr_res.confidence is not None and ocr_res.confidence < 0.40 and ocr_res.rawText:
        inconsistencies.append("Low confidence in extracted OCR textual characters")
        risk_indicators.append(RiskIndicatorDto(
            type="OCR_LOW_CONFIDENCE",
            severity="MEDIUM",
            message="Extracted document typography has low character recognition confidence"
        ))

    elapsed_ms = int((time.time() - start_time) * 1000)

    logger.info(
        "Screening pipeline finished in %d ms: Face=%s (sim=%.3f), Tampering=%s, OCR lines=%s",
        elapsed_ms,
        face_res.status,
        face_res.similarity if face_res.similarity is not None else 0.0,
        tamper_res.status,
        ocr_res.additionalFields.get("detectedLines", "0") if ocr_res.additionalFields else "0"
    )

    return AiAnalysisResponse(
        status="COMPLETED",
        ocr=ocr_res,
        tampering=tamper_res,
        faceVerification=face_res,
        imageQuality=quality_res,
        inconsistencies=inconsistencies,
        riskIndicators=risk_indicators,
        processing=ProcessingMetaDto(
            processingTimeMs=elapsed_ms,
            modelVersion="InsightFace-buffalo_sc+PaddleOCR-ONNX-v1.0"
        )
    )
