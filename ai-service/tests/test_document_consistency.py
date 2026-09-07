import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import cv2
import numpy as np
from app.services.analysis_pipeline import run_full_analysis
from app.schemas.requests import AiAnalysisRequest


def test_no_selfie_provided():
    # Synthetic document card image with text
    doc_img = np.full((300, 450, 3), 255, dtype=np.uint8)
    cv2.putText(doc_img, "SAMPLE ID", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    _, doc_bytes = cv2.imencode(".jpg", doc_img)

    req = AiAnalysisRequest(documentType="AADHAAR_CARD")
    res = run_full_analysis(doc_bytes=doc_bytes.tobytes(), selfie_bytes=None, request_dto=req)

    assert res.faceVerification.matched is False
    assert res.faceVerification.confidence == 0.0
    assert res.faceVerification.status in ["NO_SELFIE_PROVIDED", "FACE_NOT_FOUND_DOCUMENT"]
    print("[TEST 1 PASSED] No selfie provided returns matched=False and confidence=0.0")


def test_aadhaar_with_student_id_triggers_mismatch():
    # Synthetic Student ID card image
    doc_img = np.full((350, 500, 3), 255, dtype=np.uint8)
    cv2.putText(doc_img, "BRAINWARE UNIVERSITY", (30, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 0), 2)
    cv2.putText(doc_img, "STUDENT IDENTITY CARD", (30, 110), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)
    cv2.putText(doc_img, "Name: Sample Student", (30, 160), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)
    cv2.putText(doc_img, "Roll: STU/2024/042", (30, 210), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 2)
    _, doc_bytes = cv2.imencode(".jpg", doc_img)

    # Request declared as AADHAAR_CARD
    req = AiAnalysisRequest(documentType="AADHAAR_CARD")
    res = run_full_analysis(doc_bytes=doc_bytes.tobytes(), selfie_bytes=None, request_dto=req)

    print("Inconsistencies detected:", res.inconsistencies)
    print("Risk indicators detected:", [ind.type for ind in res.riskIndicators])

    has_mismatch_ind = any(ind.type == "DOCUMENT_TYPE_MISMATCH" for ind in res.riskIndicators)
    has_mismatch_inc = any("mismatch" in inc.lower() for inc in res.inconsistencies)

    assert has_mismatch_ind, "Expected DOCUMENT_TYPE_MISMATCH risk indicator!"
    assert has_mismatch_inc, "Expected mismatch explanation in inconsistencies!"
    print("[TEST 2 PASSED] Uploading student ID as Aadhaar successfully flagged DOCUMENT_TYPE_MISMATCH!")


if __name__ == "__main__":
    print("Running document format consistency tests...")
    test_no_selfie_provided()
    test_aadhaar_with_student_id_triggers_mismatch()
    print("\nALL CONSISTENCY TESTS COMPLETED SUCCESSFULLY!")

