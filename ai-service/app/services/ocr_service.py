import logging
import re
from typing import Optional, Dict, Any, List
import numpy as np

from app.models.ocr_model import get_ocr_engine
from app.schemas.responses import OcrDataDto

logger = logging.getLogger("ai_service.ocr")

# Indian Document Pattern Regexes
AADHAAR_REGEX = re.compile(r"\b(\d{4}\s\d{4}\s\d{4})\b")
PAN_REGEX = re.compile(r"\b([A-Z]{5}[0-9]{4}[A-Z])\b")
VOTER_REGEX = re.compile(r"\b([A-Z]{3}[0-9]{7})\b")
DRIVING_LICENCE_REGEX = re.compile(r"\b([A-Z]{2}[0-9]{2}\s?[0-9]{11})\b")
STUDENT_ID_REGEX = re.compile(r"\b([A-Z]{2,6}/[A-Z0-9/_-]{5,20})\b")
DOB_REGEX = re.compile(r"(?:DOB|Date of Birth|Birth)[:\s]*([0-9]{2}[/-][0-9]{2}[/-][0-9]{4}|[0-9]{4})", re.IGNORECASE)


def extract_ocr_from_image(img: Optional[np.ndarray], doc_type: Optional[str] = None) -> OcrDataDto:
    """
    Executes real PaddleOCR ONNX text extraction on the document image
    and parses structured identity fields.
    """
    if img is None:
        return OcrDataDto()

    engine = get_ocr_engine()
    # RapidOCR returns tuple (result, elapse_list)
    # where result is a list of [box, text, score]
    ocr_res, _ = engine(img)

    if not ocr_res:
        logger.info("OCR engine found no readable text on document.")
        return OcrDataDto(
            rawText="",
            confidence=0.0,
            additionalFields={"detectedLines": "0"}
        )

    lines: List[str] = []
    scores: List[float] = []

    for item in ocr_res:
        # item: [box, text, score]
        if len(item) >= 3:
            text = str(item[1]).strip()
            score = float(item[2])
            if text:
                lines.append(text)
                scores.append(score)

    full_text = "\n".join(lines)
    avg_confidence = float(np.mean(scores)) if scores else 0.0

    # Structured Field Extraction using Regex & Heuristics
    name = None
    dob = None
    doc_number = None
    additional_fields: Dict[str, str] = {}

    # 1. Search for document numbers
    pan_match = PAN_REGEX.search(full_text)
    aadhaar_match = AADHAAR_REGEX.search(full_text)
    voter_match = VOTER_REGEX.search(full_text)
    dl_match = DRIVING_LICENCE_REGEX.search(full_text)
    student_match = STUDENT_ID_REGEX.search(full_text)

    if pan_match:
        doc_number = pan_match.group(1)
        additional_fields["matchedDocFormat"] = "PAN_CARD"
    elif aadhaar_match:
        doc_number = aadhaar_match.group(1)
        additional_fields["matchedDocFormat"] = "AADHAAR_CARD"
    elif voter_match:
        doc_number = voter_match.group(1)
        additional_fields["matchedDocFormat"] = "VOTER_ID"
    elif dl_match:
        doc_number = dl_match.group(1)
        additional_fields["matchedDocFormat"] = "DRIVING_LICENCE"
    elif student_match:
        doc_number = student_match.group(1)
        additional_fields["matchedDocFormat"] = "STUDENT_ID"

    # 2. Search for Date of Birth
    dob_match = DOB_REGEX.search(full_text)
    if dob_match:
        dob = dob_match.group(1)

    # 3. Search for Name
    for i, line in enumerate(lines):
        clean_line = line.strip()
        # Look for explicit "Name:" pattern
        if re.search(r"Name\s*[:\-]\s*", clean_line, re.IGNORECASE):
            extracted = re.sub(r"Name\s*[:\-]\s*", "", clean_line, flags=re.IGNORECASE).strip()
            if extracted and len(extracted) > 2:
                name = extracted
                break
        # Look for Student name / Citizen name header
        if "STUDENT" in clean_line.upper() and i + 1 < len(lines):
            candidate = lines[i + 1].strip()
            if re.match(r"^[A-Za-z\s\.]+$", candidate) and len(candidate) > 3:
                name = candidate
                break

    # If no explicit Name: found, look for first line with purely alphabetic words
    if not name:
        for line in lines[:5]:
            clean = line.strip()
            if (
                re.match(r"^[A-Z][a-zA-Z\s\.]+$", clean)
                and len(clean) > 4
                and not any(w in clean.upper() for w in ["GOVERNMENT", "INDIA", "INCOME", "TAX", "CARD", "IDENTITY", "UNIVERSITY"])
            ):
                name = clean
                break

    additional_fields["detectedLines"] = str(len(lines))

    return OcrDataDto(
        name=name,
        dateOfBirth=dob,
        documentNumber=doc_number,
        rawText=full_text,
        confidence=round(avg_confidence, 4),
        additionalFields=additional_fields
    )
