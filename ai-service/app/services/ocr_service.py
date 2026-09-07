import logging
import re
from typing import Optional, Dict, Any, List, Tuple
import cv2
import numpy as np

from app.models.ocr_model import get_ocr_engine
from app.schemas.responses import OcrDataDto

logger = logging.getLogger("ai_service.ocr")

# Indian Document Pattern Regexes
AADHAAR_STRICT_REGEX = re.compile(r"\b(\d{4}\s\d{4}\s\d{4})\b")
AADHAAR_FLEX_REGEX = re.compile(r"\b(\d{4}[\s\-\.]?\d{4}[\s\-\.]?\d{4})\b")
AADHAAR_MASKED_REGEX = re.compile(r"\b([Xx*]{4}[\s\-]?[Xx*]{4}[\s\-]?\d{4})\b")
PAN_REGEX = re.compile(r"\b([A-Z]{5}[0-9]{4}[A-Z])\b")
PAN_NOISY_REGEX = re.compile(r"\b([A-Z]{5}[0-9OIBZS]{4}[A-Z])\b")
VOTER_REGEX = re.compile(r"\b([A-Z]{3}[0-9]{7})\b")
VOTER_FLEX_REGEX = re.compile(r"\b([A-Z]{2,4}[/-]?[0-9]{6,8})\b")
DRIVING_LICENCE_REGEX = re.compile(r"\b([A-Z]{2}[0-9]{2}[\s\-]?(?:19|20)\d{2}[\s\-]?\d{7})\b")
DRIVING_LICENCE_FLEX_REGEX = re.compile(r"\b([A-Z]{2}[-\s]?[0-9]{13,15})\b")
PASSPORT_REGEX = re.compile(r"\b([A-PR-WYa-pr-wy][1-9]\d\s?\d{4}[1-9])\b")
PASSPORT_FLEX_REGEX = re.compile(r"\b([A-Z][0-9]{7,8})\b")
STUDENT_ID_REGEX = re.compile(r"\b([A-Z]{2,6}/[A-Z0-9/_-]{4,20})\b")
STUDENT_ROLL_REGEX = re.compile(r"(?:Roll|Reg|ID|Enrollment|Admission)\s*(?:No|Number)?[:\s\-]*([A-Z0-9/_-]{4,25})", re.IGNORECASE)
RATION_CARD_REGEX = re.compile(r"\b((?:NFSA|PDS|RC)[/-]?[0-9A-Z]{5,16}|\d{10,12})\b", re.IGNORECASE)
ABHA_REGEX = re.compile(r"\b(\d{2}-\d{4}-\d{4}-\d{4}|\d{14})\b")
BIRTH_CERT_REGEX = re.compile(r"\b((?:CRS|B|D|REG)[/-]?[0-9A-Z/_-]{5,20})\b", re.IGNORECASE)

# DOB Patterns
DOB_LABEL_REGEX = re.compile(
    r"(?:DOB|D\.O\.B|Date of Birth|Birth|जन्म\s*तारीख|जन्म\s*तिथि|DOB/जन्म)[:\s\-]*([0-9]{2}[/\-\.][0-9]{2}[/\-\.][0-9]{4}|[0-9]{4})",
    re.IGNORECASE
)
STANDALONE_DATE_REGEX = re.compile(r"\b(0?[1-9]|[12][0-9]|3[01])[/\-\.](0?[1-9]|1[012])[/\-\.](19\d{2}|20[0-2]\d)\b")
YOB_REGEX = re.compile(r"(?:Year of Birth|YOB|जन्म\s*वर्ष)[:\s\-]*([12][90]\d{2})", re.IGNORECASE)

# Document boilerplate words to ignore when extracting names
BOILERPLATE_WORDS = {
    "GOVERNMENT", "INDIA", "BHARAT", "SARKAR", "INCOME", "TAX", "DEPARTMENT",
    "AADHAAR", "UNIQUE", "IDENTIFICATION", "AUTHORITY", "MALE", "FEMALE",
    "PURUSH", "STRI", "SIGNATURE", "CARD", "ACCOUNT", "ELECTION", "COMMISSION",
    "TRANSPORT", "UNION", "REPUBLIC", "ENROLLMENT", "FATHER", "MOTHER", "HUSBAND",
    "ADDRESS", "STATE", "VALID", "ISSUED", "VALIDITY", "EXPIRY", "DATE",
    "PHOTO", "OFFICIAL", "NATIONAL", "PERMANENT", "NUMBER", "UNIVERSITY",
    "COLLEGE", "SCHOOL", "INSTITUTE", "STUDENT", "IDENTITY", "DL", "DOB"
}


def preprocess_image_for_ocr(img: np.ndarray) -> np.ndarray:
    """
    Applies resolution scaling and CLAHE contrast enhancement in LAB color space.
    Significantly enhances OCR readability of micro-printed document texts and IDs.
    """
    if img is None:
        return None

    h, w = img.shape[:2]

    # 1. Scale up small images for better small-font OCR recognition
    target_img = img
    if max(h, w) < 1200:
        scale = 1200.0 / max(h, w)
        target_img = cv2.resize(img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)
    elif max(h, w) > 2600:
        scale = 2600.0 / max(h, w)
        target_img = cv2.resize(img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)

    # 2. Contrast enhancement using CLAHE in LAB color space
    try:
        lab = cv2.cvtColor(target_img, cv2.COLOR_BGR2LAB)
        l_channel, a_channel, b_channel = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=2.2, tileGridSize=(8, 8))
        cl = clahe.apply(l_channel)
        merged = cv2.merge((cl, a_channel, b_channel))
        enhanced = cv2.cvtColor(merged, cv2.COLOR_LAB2BGR)
        return enhanced
    except Exception as ex:
        logger.warn("CLAHE preprocessing failed, falling back to original: %s", ex)
        return target_img


def clean_aadhaar_number(raw_str: str) -> str:
    """Formats 12-digit Aadhaar as 'XXXX XXXX XXXX'."""
    digits = re.sub(r"\D", "", raw_str)
    if len(digits) == 12:
        return f"{digits[0:4]} {digits[4:8]} {digits[8:12]}"
    return raw_str.strip()


def clean_pan_number(raw_str: str) -> str:
    """Corrects common OCR digit-letter misreads in 10-char PAN string."""
    s = raw_str.strip().upper()
    if len(s) == 10:
        # First 5 characters must be letters
        alpha_part = s[:5]
        # Middle 4 characters must be digits
        digit_part = list(s[5:9])
        for idx, ch in enumerate(digit_part):
            if ch == 'O' or ch == 'Q':
                digit_part[idx] = '0'
            elif ch == 'I' or ch == 'L':
                digit_part[idx] = '1'
            elif ch == 'Z':
                digit_part[idx] = '2'
            elif ch == 'S':
                digit_part[idx] = '5'
            elif ch == 'B':
                digit_part[idx] = '8'
        last_char = s[9]
        return alpha_part + "".join(digit_part) + last_char
    return s


def clean_extracted_name(name_str: str) -> Optional[str]:
    """Cleans punctuation, digits, and normalizes capitalization of extracted name."""
    if not name_str:
        return None

    # Remove labels like "Name:", "Citizen Name:"
    cleaned = re.sub(r"^(?:Name|Citizen Name|Elector\'?s Name|Student Name|Holder\'?s Name|नाम)\s*[:\-]*\s*", "", name_str, flags=re.IGNORECASE)
    # Remove digits and weird symbols
    cleaned = re.sub(r"[0-9\<\>\{\}\[\]\(\)\:\;\=\_\-\+\*\#\@\!\|\/\\]", " ", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()

    # If it's too short or contains only boilerplate words
    words = [w for w in cleaned.split() if len(w) > 1 and w.upper() not in BOILERPLATE_WORDS]
    if not words or len(" ".join(words)) < 3:
        return None

    candidate = " ".join(words)
    # If all uppercase, format nicely into Title Case
    if candidate.isupper():
        candidate = candidate.title()

    return candidate


def extract_ocr_from_image(img: Optional[np.ndarray], doc_type: Optional[str] = None) -> OcrDataDto:
    """
    Executes high-accuracy PaddleOCR ONNX text extraction on the document image
    with intelligent multi-pass preprocessing and deep structured identity parsing.
    """
    if img is None:
        return OcrDataDto()

    engine = get_ocr_engine()

    # Pass 1: Enhanced contrast image
    enhanced_img = preprocess_image_for_ocr(img)
    ocr_res, _ = engine(enhanced_img)

    # Pass 2 Fallback: If contrast-enhanced detection found few lines, try raw image
    if not ocr_res or len(ocr_res) < 3:
        raw_res, _ = engine(img)
        if raw_res and (not ocr_res or len(raw_res) > len(ocr_res)):
            ocr_res = raw_res

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
        if len(item) >= 3:
            text = str(item[1]).strip()
            score = float(item[2])
            if text:
                lines.append(text)
                scores.append(score)

    full_text = "\n".join(lines)
    avg_confidence = float(np.mean(scores)) if scores else 0.0

    # Structured Field Extraction
    name: Optional[str] = None
    dob: Optional[str] = None
    doc_number: Optional[str] = None
    additional_fields: Dict[str, str] = {}

    # -------------------------------------------------------------
    # 1. DOCUMENT IDENTIFIER EXTRACTION
    # -------------------------------------------------------------
    # Try PAN Card
    pan_match = PAN_REGEX.search(full_text)
    if not pan_match:
        pan_match = PAN_NOISY_REGEX.search(full_text)
    if pan_match:
        doc_number = clean_pan_number(pan_match.group(1))
        additional_fields["matchedDocFormat"] = "PAN_CARD"

    # Try Aadhaar Card
    if not doc_number:
        aadhaar_match = AADHAAR_STRICT_REGEX.search(full_text)
        if not aadhaar_match:
            aadhaar_match = AADHAAR_FLEX_REGEX.search(full_text)
        if not aadhaar_match:
            aadhaar_match = AADHAAR_MASKED_REGEX.search(full_text)
        if aadhaar_match:
            doc_number = clean_aadhaar_number(aadhaar_match.group(1))
            additional_fields["matchedDocFormat"] = "AADHAAR_CARD"

    # Try Voter ID
    if not doc_number:
        voter_match = VOTER_REGEX.search(full_text)
        if not voter_match:
            voter_match = VOTER_FLEX_REGEX.search(full_text)
        if voter_match:
            doc_number = voter_match.group(1).upper()
            additional_fields["matchedDocFormat"] = "VOTER_ID"

    # Try Driving Licence
    if not doc_number:
        dl_match = DRIVING_LICENCE_REGEX.search(full_text)
        if not dl_match:
            dl_match = DRIVING_LICENCE_FLEX_REGEX.search(full_text)
        if dl_match:
            doc_number = dl_match.group(1).upper().replace(" ", "-")
            additional_fields["matchedDocFormat"] = "DRIVING_LICENCE"

    # Try Passport
    if not doc_number:
        passport_match = PASSPORT_REGEX.search(full_text)
        if not passport_match:
            passport_match = PASSPORT_FLEX_REGEX.search(full_text)
        if passport_match:
            doc_number = passport_match.group(1).upper()
            additional_fields["matchedDocFormat"] = "PASSPORT"

    # Try Student ID
    if not doc_number:
        student_match = STUDENT_ID_REGEX.search(full_text)
        if not student_match:
            student_match = STUDENT_ROLL_REGEX.search(full_text)
        if student_match:
            doc_number = student_match.group(1)
            additional_fields["matchedDocFormat"] = "STUDENT_ID"

    # Try Ration, ABHA, Birth Certificate
    if not doc_number:
        for pattern, fmt in [
            (RATION_CARD_REGEX, "RATION_CARD"),
            (ABHA_REGEX, "ABHA_CARD"),
            (BIRTH_CERT_REGEX, "BIRTH_CERTIFICATE")
        ]:
            m = pattern.search(full_text)
            if m:
                doc_number = m.group(1)
                additional_fields["matchedDocFormat"] = fmt
                break

    # -------------------------------------------------------------
    # 2. DATE OF BIRTH (DOB) EXTRACTION
    # -------------------------------------------------------------
    # 2a. Look for explicit DOB label on same or adjacent lines
    for i, line in enumerate(lines):
        clean_line = line.strip()
        label_match = DOB_LABEL_REGEX.search(clean_line)
        if label_match:
            val = label_match.group(1).replace("-", "/").replace(".", "/")
            if len(val) >= 4:
                dob = val
                break
        # Label alone on line -> check next line
        if any(lbl in clean_line.upper() for lbl in ["DOB", "DATE OF BIRTH", "BIRTH", "जन्म तिथि"]):
            if i + 1 < len(lines):
                next_line = lines[i + 1].strip()
                date_m = STANDALONE_DATE_REGEX.search(next_line)
                if date_m:
                    dob = date_m.group(0).replace("-", "/").replace(".", "/")
                    break

    # 2b. Standalone date search across document if no labelled DOB found
    if not dob:
        all_dates: List[str] = []
        for line in lines:
            matches = STANDALONE_DATE_REGEX.findall(line)
            for m in matches:
                # m is tuple (day, month, year)
                day, month, year = m[0], m[1], m[2]
                y_int = int(year)
                # Birth years are logically between 1920 and current year - 1
                if 1920 <= y_int <= 2024:
                    all_dates.append(f"{day.zfill(2)}/{month.zfill(2)}/{year}")

        if all_dates:
            # Pick date with earliest year as the DOB (as opposed to issue/expiry dates)
            all_dates.sort(key=lambda d: int(d.split("/")[-1]))
            dob = all_dates[0]

    # 2c. Year of birth only fallback (common in older Aadhaar cards)
    if not dob:
        yob_match = YOB_REGEX.search(full_text)
        if yob_match:
            dob = yob_match.group(1)

    # -------------------------------------------------------------
    # 3. CITIZEN NAME EXTRACTION
    # -------------------------------------------------------------
    # 3a. Explicit Name label search (e.g. "Name: Sayak Dutta" or "Name" followed by next line)
    for i, line in enumerate(lines):
        clean_line = line.strip()

        # Check "Name:" or "Name -" pattern on current line
        if re.search(r"^(?:Name|Citizen Name|Elector\'?s Name|Student Name|Holder\'?s Name|नाम)\s*[:\-]\s*", clean_line, re.IGNORECASE):
            extracted = clean_extracted_name(clean_line)
            if extracted:
                name = extracted
                break

        # Check label on current line and actual name on line i+1 (Standard on PAN cards & ID badges)
        if re.match(r"^(?:Name|Citizen Name|Student Name|Holder\'?s Name|नाम)$", clean_line, re.IGNORECASE):
            if i + 1 < len(lines):
                candidate = clean_extracted_name(lines[i + 1])
                if candidate:
                    name = candidate
                    break

    # 3b. Aadhaar Card Structural Heuristic:
    # On Aadhaar cards, the English name is almost always directly above the DOB line
    if not name and dob:
        dob_index = -1
        for i, line in enumerate(lines):
            if dob in line or "DOB" in line.upper() or "DATE OF BIRTH" in line.upper() or "जन्म" in line:
                dob_index = i
                break

        if dob_index > 0:
            # Check 1 or 2 lines above the DOB line
            for offset in [1, 2]:
                candidate_idx = dob_index - offset
                if candidate_idx >= 0:
                    cand = lines[candidate_idx].strip()
                    cleaned = clean_extracted_name(cand)
                    if cleaned and re.match(r"^[A-Za-z\s\.\']+$", cleaned) and len(cleaned.split()) >= 2:
                        name = cleaned
                        break

    # 3c. PAN Card Structural Heuristic:
    # Look for "Father's Name" -> Citizen name is directly above Father's Name
    if not name:
        for i, line in enumerate(lines):
            if "FATHER" in line.upper():
                if i - 1 >= 0:
                    candidate = clean_extracted_name(lines[i - 1])
                    if candidate and re.match(r"^[A-Za-z\s\.\']+$", candidate):
                        name = candidate
                        break

    # 3d. Fallback: First non-boilerplate line with 2+ English alphabetic words
    if not name:
        for line in lines[:8]:
            clean = line.strip()
            # Must consist of alphabetic words and not be a government/header banner
            if (
                re.match(r"^[A-Za-z\s\.\']+$", clean)
                and len(clean) >= 4
                and not any(w in clean.upper() for w in BOILERPLATE_WORDS)
            ):
                candidate = clean_extracted_name(clean)
                if candidate and len(candidate.split()) >= 2:
                    name = candidate
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
