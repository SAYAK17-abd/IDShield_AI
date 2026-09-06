import logging
import numpy as np
from typing import Tuple, Optional
import cv2

from app.models.face_model import get_face_app
from app.schemas.responses import FaceVerificationDto

logger = logging.getLogger("ai_service.face_verification")

# Pretrained ArcFace (buffalo_sc w600k_mbf) calibrated threshold
ARCFACE_MATCH_THRESHOLD = 0.45
ARCFACE_BORDERLINE_THRESHOLD = 0.32


def calculate_cosine_similarity(emb1: np.ndarray, emb2: np.ndarray) -> float:
    """Calculates cosine similarity between two face embeddings."""
    norm1 = np.linalg.norm(emb1)
    norm2 = np.linalg.norm(emb2)
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return float(np.dot(emb1, emb2) / (norm1 * norm2))


def calibrate_confidence(similarity: float, threshold: float = ARCFACE_MATCH_THRESHOLD) -> float:
    """
    Calibrates raw cosine similarity into an explainable verification confidence score [0.0, 1.0].
    Uses a sigmoid centered at the operational threshold.
    """
    # Scale factor k determines steepness around threshold
    k = 10.0
    logit = k * (similarity - threshold)
    confidence = 1.0 / (1.0 + np.exp(-logit))
    return float(np.clip(confidence, 0.01, 0.99))


def verify_faces(
    doc_img: Optional[np.ndarray],
    selfie_img: Optional[np.ndarray]
) -> FaceVerificationDto:
    """
    Performs end-to-end face detection, alignment, embedding extraction,
    and cosine similarity verification between document photo and live selfie.
    """
    if doc_img is None:
        return FaceVerificationDto(
            matched=False,
            confidence=0.0,
            status="DOCUMENT_EMPTY",
            reason="Document image could not be loaded or parsed"
        )

    app = get_face_app()

    # 1. Detect faces in document
    doc_faces = app.get(doc_img)
    doc_face_count = len(doc_faces)
    doc_detected = doc_face_count > 0

    # If no selfie was provided, return document face detection status
    if selfie_img is None:
        return FaceVerificationDto(
            matched=True if doc_detected else False,
            confidence=0.5 if doc_detected else 0.0,
            status="DOCUMENT_FACE_DETECTED" if doc_detected else "FACE_NOT_FOUND_DOCUMENT",
            documentFaceDetected=doc_detected,
            selfieFaceDetected=False,
            documentFaceCount=doc_face_count,
            selfieFaceCount=0,
            threshold=ARCFACE_MATCH_THRESHOLD,
            reason="Document face detected; no biometric reference selfie provided for 1:1 comparison"
            if doc_detected else "No usable face detected on identity document"
        )

    # 2. Detect faces in selfie
    selfie_faces = app.get(selfie_img)
    selfie_face_count = len(selfie_faces)
    selfie_detected = selfie_face_count > 0

    # Edge Case A: No face in document
    if doc_face_count == 0:
        return FaceVerificationDto(
            matched=False,
            confidence=0.0,
            status="FACE_NOT_FOUND_DOCUMENT",
            documentFaceDetected=False,
            selfieFaceDetected=selfie_detected,
            documentFaceCount=0,
            selfieFaceCount=selfie_face_count,
            threshold=ARCFACE_MATCH_THRESHOLD,
            reason="No face detected in the uploaded identity document"
        )

    # Edge Case B: No face in selfie
    if selfie_face_count == 0:
        return FaceVerificationDto(
            matched=False,
            confidence=0.0,
            status="FACE_NOT_FOUND_SELFIE",
            documentFaceDetected=True,
            selfieFaceDetected=False,
            documentFaceCount=doc_face_count,
            selfieFaceCount=0,
            threshold=ARCFACE_MATCH_THRESHOLD,
            reason="No face detected in the live selfie biometric capture"
        )

    # Edge Case C: Multiple faces detected in document or selfie
    if doc_face_count > 1 or selfie_face_count > 1:
        return FaceVerificationDto(
            matched=False,
            confidence=0.20,
            status="MULTIPLE_FACES_DETECTED",
            documentFaceDetected=True,
            selfieFaceDetected=True,
            documentFaceCount=doc_face_count,
            selfieFaceCount=selfie_face_count,
            threshold=ARCFACE_MATCH_THRESHOLD,
            reason=f"Multiple faces detected (Doc: {doc_face_count}, Selfie: {selfie_face_count}); flagged for manual review"
        )

    # Case D: Exactly 1 face in each image -> Run real ArcFace feature embedding comparison
    doc_face = doc_faces[0]
    selfie_face = selfie_faces[0]

    emb_doc = doc_face.embedding
    emb_selfie = selfie_face.embedding

    # Raw cosine similarity between 512-d normalized feature vectors
    similarity = calculate_cosine_similarity(emb_doc, emb_selfie)
    confidence = calibrate_confidence(similarity, ARCFACE_MATCH_THRESHOLD)

    if similarity >= ARCFACE_MATCH_THRESHOLD:
        status = "MATCH"
        matched = True
        reason = f"Facial biometric match confirmed (Cosine Similarity: {similarity:.3f} >= Threshold: {ARCFACE_MATCH_THRESHOLD:.2f})"
    elif similarity >= ARCFACE_BORDERLINE_THRESHOLD:
        status = "UNCERTAIN"
        matched = False
        reason = f"Borderline biometric similarity ({similarity:.3f}); officer review required"
    else:
        status = "MISMATCH"
        matched = False
        reason = f"Biometric mismatch: facial embeddings are substantially different ({similarity:.3f} < Threshold: {ARCFACE_MATCH_THRESHOLD:.2f})"

    logger.info(
        "Face verification result: status=%s, similarity=%.4f, confidence=%.4f, matched=%s",
        status, similarity, confidence, matched
    )

    return FaceVerificationDto(
        matched=matched,
        confidence=round(confidence, 4),
        status=status,
        similarity=round(similarity, 4),
        threshold=ARCFACE_MATCH_THRESHOLD,
        documentFaceDetected=True,
        selfieFaceDetected=True,
        documentFaceCount=1,
        selfieFaceCount=1,
        reason=reason
    )
