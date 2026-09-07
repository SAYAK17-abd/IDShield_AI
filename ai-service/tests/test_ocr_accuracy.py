import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import cv2
import numpy as np
from app.services.ocr_service import (
    extract_ocr_from_image,
    clean_aadhaar_number,
    clean_pan_number,
    clean_extracted_name,
    preprocess_image_for_ocr
)
from app.schemas.responses import OcrDataDto


def test_clean_helpers():
    # Test Aadhaar formatting
    assert clean_aadhaar_number("123456789012") == "1234 5678 9012"
    assert clean_aadhaar_number("1234 5678 9012") == "1234 5678 9012"

    # Test PAN cleaning (OCR confusion O/0, I/1)
    assert clean_pan_number("ABCDE1234F") == "ABCDE1234F"
    assert clean_pan_number("ABCDEO234F") == "ABCDE0234F"

    # Test Name cleaning
    assert clean_extracted_name("Name: Sayak Dutta") == "Sayak Dutta"
    assert clean_extracted_name("SAYAK DUTTA") == "Sayak Dutta"
    assert clean_extracted_name("GOVERNMENT OF INDIA") is None
    print("[TEST PASSED] Helper cleaning and normalization functions work accurately.")


def test_synthetic_aadhaar_ocr():
    # Generate realistic synthetic Aadhaar card image
    img = np.full((500, 800, 3), 255, dtype=np.uint8)
    cv2.putText(img, "GOVERNMENT OF INDIA", (180, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 0), 2)
    cv2.putText(img, "Sayak Dutta", (180, 160), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img, "DOB: 14/08/2002", (180, 220), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 0), 2)
    cv2.putText(img, "MALE", (180, 270), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 0), 2)
    cv2.putText(img, "9928 3847 1029", (220, 380), cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 0), 3)

    ocr_dto = extract_ocr_from_image(img, "AADHAAR_CARD")
    print("\nAadhaar OCR Results:")
    print("  Extracted Name:", ocr_dto.name)
    print("  Extracted Document Number:", ocr_dto.documentNumber)
    print("  Extracted DOB:", ocr_dto.dateOfBirth)
    print("  Raw text:\n", ocr_dto.rawText)

    assert ocr_dto.documentNumber == "9928 3847 1029", f"Expected Aadhaar number, got {ocr_dto.documentNumber}"
    assert ocr_dto.dateOfBirth == "14/08/2002", f"Expected DOB 14/08/2002, got {ocr_dto.dateOfBirth}"
    assert ocr_dto.name == "Sayak Dutta", f"Expected Sayak Dutta, got {ocr_dto.name}"
    print("[TEST PASSED] Synthetic Aadhaar Card accurately parsed with real details.")


def test_synthetic_pan_ocr():
    # Generate realistic synthetic PAN card image
    img = np.full((500, 800, 3), 255, dtype=np.uint8)
    cv2.putText(img, "INCOME TAX DEPARTMENT", (180, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img, "ABCDE1234F", (250, 130), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 0, 0), 2)
    cv2.putText(img, "NAME", (180, 180), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    cv2.putText(img, "SAYAK DUTTA", (180, 220), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img, "FATHER'S NAME", (180, 270), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 0), 1)
    cv2.putText(img, "RAJESH DUTTA", (180, 310), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 0), 2)
    cv2.putText(img, "14/08/2002", (180, 380), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 0), 2)

    ocr_dto = extract_ocr_from_image(img, "PAN_CARD")
    print("\nPAN OCR Results:")
    print("  Extracted Name:", ocr_dto.name)
    print("  Extracted Document Number:", ocr_dto.documentNumber)
    print("  Extracted DOB:", ocr_dto.dateOfBirth)

    assert ocr_dto.documentNumber == "ABCDE1234F", f"Expected PAN ABCDE1234F, got {ocr_dto.documentNumber}"
    assert ocr_dto.dateOfBirth == "14/08/2002", f"Expected DOB 14/08/2002, got {ocr_dto.dateOfBirth}"
    assert ocr_dto.name == "Sayak Dutta", f"Expected Sayak Dutta, got {ocr_dto.name}"
    print("[TEST PASSED] Synthetic PAN Card accurately parsed with real details.")


if __name__ == "__main__":
    print("Running OCR Scanning Accuracy Tests...")
    test_clean_helpers()
    test_synthetic_aadhaar_ocr()
    test_synthetic_pan_ocr()
    print("\nALL OCR ACCURACY TESTS PASSED!")

