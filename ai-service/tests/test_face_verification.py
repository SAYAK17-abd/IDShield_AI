import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
import numpy as np
import cv2
from app.services.face_verification import (
    calculate_cosine_similarity,
    calibrate_confidence,
    verify_faces,
    ARCFACE_MATCH_THRESHOLD
)
from app.services.tamper_detection import calculate_ela, detect_noise_inconsistency
from app.services.image_quality import assess_image_quality


def test_cosine_similarity_identical_vectors():
    vec = np.random.randn(512).astype(np.float32)
    sim = calculate_cosine_similarity(vec, vec)
    assert abs(sim - 1.0) < 1e-4


def test_cosine_similarity_orthogonal_vectors():
    vec1 = np.zeros(512, dtype=np.float32)
    vec2 = np.zeros(512, dtype=np.float32)
    vec1[0] = 1.0
    vec2[1] = 1.0
    sim = calculate_cosine_similarity(vec1, vec2)
    assert abs(sim - 0.0) < 1e-4


def test_confidence_calibration():
    # Above threshold -> high confidence
    high_conf = calibrate_confidence(0.70, ARCFACE_MATCH_THRESHOLD)
    assert high_conf > 0.80

    # Near threshold -> ~0.50
    mid_conf = calibrate_confidence(ARCFACE_MATCH_THRESHOLD, ARCFACE_MATCH_THRESHOLD)
    assert abs(mid_conf - 0.50) < 0.05

    # Low similarity -> low confidence
    low_conf = calibrate_confidence(0.15, ARCFACE_MATCH_THRESHOLD)
    assert low_conf < 0.15


def test_face_verification_no_face():
    # Blank black image has no human face
    blank = np.zeros((400, 400, 3), dtype=np.uint8)
    res = verify_faces(blank, blank)
    assert res.status in ["FACE_NOT_FOUND_DOCUMENT", "FACE_NOT_FOUND_SELFIE"]
    assert res.matched is False


def test_image_quality_assessment():
    # Crisp gradient image
    img = np.zeros((500, 500, 3), dtype=np.uint8)
    cv2.putText(img, "TEST DOCUMENT", (50, 100), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (255, 255, 255), 2)
    q = assess_image_quality(img)
    assert q.score > 0.0
    assert q.resolution == "MEDIUM" or q.resolution == "GOOD"


if __name__ == "__main__":
    print("Running face verification and forensic tests...")
    test_cosine_similarity_identical_vectors()
    test_cosine_similarity_orthogonal_vectors()
    test_confidence_calibration()
    test_face_verification_no_face()
    test_image_quality_assessment()
    print("All algorithmic tests PASSED!")
