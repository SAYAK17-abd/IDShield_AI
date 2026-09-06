import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import cv2
import numpy as np
from insightface.data import get_image
from insightface.app import FaceAnalysis

from app.services.face_verification import verify_faces, calculate_cosine_similarity
from app.services.analysis_pipeline import run_full_analysis
from app.schemas.requests import AiAnalysisRequest

def run_acceptance_tests():
    print("=================================================================")
    print("  SIH26188 AI Module: Real Face Biometric Acceptance Tests")
    print("=================================================================")

    app = FaceAnalysis(name="buffalo_sc", providers=["CPUExecutionProvider"])
    app.prepare(ctx_id=0, det_size=(640, 640))

    group_img = get_image("t1")
    faces = app.get(group_img)
    assert len(faces) >= 2, "Test requires at least 2 distinct faces in t1"

    # Crop Person A
    bbox_a = faces[0].bbox.astype(int)
    # Add margin
    pad_a = 40
    h, w = group_img.shape[:2]
    y1_a, y2_a = max(0, bbox_a[1] - pad_a), min(h, bbox_a[3] + pad_a)
    x1_a, x2_a = max(0, bbox_a[0] - pad_a), min(w, bbox_a[2] + pad_a)
    person_a_crop = group_img[y1_a:y2_a, x1_a:x2_a].copy()

    # Crop Person B (different human)
    bbox_b = faces[1].bbox.astype(int)
    pad_b = 40
    y1_b, y2_b = max(0, bbox_b[1] - pad_b), min(h, bbox_b[3] + pad_b)
    x1_b, x2_b = max(0, bbox_b[0] - pad_b), min(w, bbox_b[2] + pad_b)
    person_b_crop = group_img[y1_b:y2_b, x1_b:x2_b].copy()

    print(f"Extracted Person A crop: {person_a_crop.shape}")
    print(f"Extracted Person B crop: {person_b_crop.shape}")

    # TEST 1: SAME PERSON (Person A as document photo + Person A as live selfie)
    print("\n[TEST 1: SAME PERSON VERIFICATION]")
    # Slight color/scale shift for realistic selfie variance
    selfie_a = cv2.resize(person_a_crop, (250, 250))
    selfie_a = cv2.convertScaleAbs(selfie_a, alpha=1.05, beta=5)

    res_same = verify_faces(person_a_crop, selfie_a)
    print(f"  Result Status: {res_same.status}")
    print(f"  Matched: {res_same.matched}")
    print(f"  Cosine Similarity: {res_same.similarity}")
    print(f"  Calibrated Confidence: {res_same.confidence:.2%}")
    print(f"  Reason: {res_same.reason}")

    assert res_same.matched is True, f"Expected MATCH for same person, got {res_same.status}"
    assert res_same.similarity >= 0.45, f"Expected similarity >= 0.45 for same person, got {res_same.similarity}"
    print("  >>> TEST 1 PASSED: SAME PERSON CORRECTLY MATCHED! <<<")

    # TEST 2: DIFFERENT PERSONS (Person A as document photo + Person B as live selfie)
    # THIS WAS THE CRITICAL FAILING TEST IN PREVIOUS DEMO
    print("\n[TEST 2: DIFFERENT PERSONS VERIFICATION (CRITICAL FIX)]")
    res_diff = verify_faces(person_a_crop, person_b_crop)
    print(f"  Result Status: {res_diff.status}")
    print(f"  Matched: {res_diff.matched}")
    print(f"  Cosine Similarity: {res_diff.similarity}")
    print(f"  Calibrated Confidence: {res_diff.confidence:.2%}")
    print(f"  Reason: {res_diff.reason}")

    assert res_diff.matched is False, f"Expected MISMATCH for different persons, got {res_diff.status}"
    assert res_diff.similarity < 0.35, f"Expected similarity < 0.35 for different persons, got {res_diff.similarity}"
    assert res_diff.confidence < 0.30, f"Expected confidence < 0.30 for different persons, got {res_diff.confidence}"
    print("  >>> TEST 2 PASSED: DIFFERENT PERSONS CORRECTLY FLAGGED AS MISMATCH! <<<")

    # TEST 3: FULL END-TO-END PIPELINE SYNTHESIS
    print("\n[TEST 3: FULL PIPELINE SYNTHESIS (DIFFERENT PERSONS)]")
    _, doc_buf = cv2.imencode(".png", person_a_crop)
    _, selfie_buf = cv2.imencode(".png", person_b_crop)

    pipeline_res = run_full_analysis(
        doc_bytes=doc_buf.tobytes(),
        selfie_bytes=selfie_buf.tobytes()
    )

    print(f"  Pipeline Status: {pipeline_res.status}")
    print(f"  Face Verification Status: {pipeline_res.faceVerification.status}")
    print(f"  Face Matched: {pipeline_res.faceVerification.matched}")
    print(f"  Face Cosine Sim: {pipeline_res.faceVerification.similarity}")
    print(f"  Inconsistencies: {pipeline_res.inconsistencies}")
    print(f"  Risk Indicators: {[ind.dict() for ind in pipeline_res.riskIndicators]}")

    assert pipeline_res.faceVerification.matched is False
    assert any(ind.type == "FACE_MISMATCH" and ind.severity == "HIGH" for ind in pipeline_res.riskIndicators)
    assert any("Face does not match" in inc for inc in pipeline_res.inconsistencies)
    print("  >>> TEST 3 PASSED: PIPELINE OUTPUTS HIGH SEVERITY FACE_MISMATCH SIGNAL! <<<")

    print("\n=================================================================")
    print("  ALL 3 CRITICAL BIOMETRIC ACCEPTANCE TESTS PASSED SUCCESSFULLY!  ")
    print("=================================================================")

if __name__ == "__main__":
    run_acceptance_tests()
