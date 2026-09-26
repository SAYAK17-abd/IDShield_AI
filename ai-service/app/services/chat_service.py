import time
from typing import List, Dict, Any, Optional
from datetime import datetime
from app.schemas.requests import ChatRequest
from app.schemas.responses import ChatResponse

# Static Statutory & Technical Knowledge Base
KNOWLEDGE_BASE = {
    "AADHAAR": {
        "title": "UIDAI Aadhaar Card Specifications",
        "authority": "Unique Identification Authority of India (UIDAI)",
        "guidelines": [
            "Must be a 12-digit Unique Identification Number (UID).",
            "Accepted formats include original PVC card, physical letter, and digitally signed e-Aadhaar PDF with valid UIDAI certificate.",
            "Masked Aadhaar (showing only the last 4 digits XXXX-XXXX-1234) is legally valid and encouraged under Section 4 of the Aadhaar Act 2016.",
            "All four card corners must be clearly visible; photo should be free of flash glare and reflections."
        ],
        "remediation": [
            "Download an authentic digitally signed e-Aadhaar directly from https://eaadhaar.uidai.gov.in.",
            "Ensure the photograph on the card is sharp and unobstructed by holograms.",
            "Do not apply digital filters, beauty retouching, or external watermarks before upload."
        ]
    },
    "PAN": {
        "title": "Permanent Account Number (PAN) Card",
        "authority": "Income Tax Department / Central Board of Direct Taxes (CBDT)",
        "guidelines": [
            "Must have a valid 10-character alphanumeric PAN format: 5 letters, 4 digits, 1 check letter (e.g. ABCDE1234F).",
            "Must feature the official Emblem of India or Income Tax Department crest and government security hologram.",
            "Date of birth on the document must strictly match the date provided during application."
        ],
        "remediation": [
            "Submit an unaltered camera capture of your physical PAN card or download the signed e-PAN from NSDL/Protean or UTIITSL.",
            "Ensure the 10-character PAN number and cardholder signature strip are legible."
        ]
    },
    "VOTER_ID": {
        "title": "Elector's Photo Identity Card (EPIC)",
        "authority": "Election Commission of India (ECI)",
        "guidelines": [
            "Features a 10-character alphanumeric EPIC number (e.g., ABC1234567).",
            "Must contain the official hologram and issuing Electoral Registration Officer seal."
        ],
        "remediation": [
            "Download an authentic e-EPIC from https://voters.eci.gov.in.",
            "Ensure the EPIC alphanumeric number and state emblem are clear."
        ]
    },
    "PASSPORT": {
        "title": "Indian Passport",
        "authority": "Ministry of External Affairs (MEA), Consular, Passport & Visa Division",
        "guidelines": [
            "Must display standard 8-character alphanumeric passport number (1 letter followed by 7 digits).",
            "Must feature readable 2-line Machine Readable Zone (MRZ) formatted to ICAO Doc 9303 Type P standards."
        ],
        "remediation": [
            "Capture the full biographical page including the 2-line MRZ zone at the bottom.",
            "Flatten the passport completely to avoid shadow cast from the spine binding."
        ]
    }
}


def generate_assistant_response(request: ChatRequest) -> ChatResponse:
    """
    RAG-grounded contextual conversational assistant:
    1. Evaluates user inquiry intent
    2. Injects user verification report context (if user is screened)
    3. Retrieves statutory guidelines and produces explainable remediation steps
    """
    user_msg = (request.message or "").strip()
    msg_lower = user_msg.lower()
    doc_type = (request.documentType or "").upper()
    ctx = request.verificationContext or {}

    reply_paragraphs: List[str] = []
    remediation: List[str] = []
    guidelines: List[str] = []

    # 1. Check for specific Case Inquiry (e.g. "Why was my document flagged?", "What is my status?")
    is_status_inquiry = any(k in msg_lower for k in ["why", "flag", "reject", "status", "score", "risk", "mismatch", "failed", "pending", "review"])
    
    if ctx and is_status_inquiry:
        risk_score = ctx.get("riskScore", 0)
        risk_level = ctx.get("riskLevel", "LOW")
        investigation_status = ctx.get("investigationStatus", "COMPLETED")
        reasons = ctx.get("reasons", [])
        inconsistencies = ctx.get("inconsistencies", [])
        tampering_detected = ctx.get("tamperingDetected", False)
        face_matched = ctx.get("faceMatched", True)
        face_conf = ctx.get("faceMatchConfidence", 0.0)

        reply_paragraphs.append(f"Here is the forensic breakdown for your verification case (Risk Score: **{risk_score}/100** • Level: **{risk_level}** • Current Status: **{investigation_status}**):")

        if risk_level == "LOW" or investigation_status == "COMPLETED":
            reply_paragraphs.append("✅ **Auto-Approved**: Your document passed cryptographic, layout, and biometric screening with no significant anomalies. Your identity is verified.")
        else:
            reply_paragraphs.append("⚠️ **Review Required / Anomaly Detected**: The automated screening engine detected one or more risk signals that require human verification:")
            for r in reasons[:3]:
                reply_paragraphs.append(f"• **Finding**: {r}")

            if tampering_detected:
                remediation.append("Do not edit or compress your document through image messaging apps (e.g., WhatsApp). Upload the direct camera capture or official digital PDF.")
            if not face_matched and face_conf > 0:
                remediation.append("Ensure your reference selfie is captured frontally in bright, balanced lighting without eyeglasses, masks, or headwear.")
            if any("mismatch" in s.lower() for s in inconsistencies):
                remediation.append("Double-check that the document type you selected in the portal matches the physical identity card you uploaded.")

        if investigation_status == "REVIEW_REQUIRED":
            reply_paragraphs.append("\n👮 **Officer Workflow**: Your file is currently queued in the Forensic Investigation Officer Dashboard. An authorized officer will examine the flagged artifacts and confirm the final decision.")

    # 2. Document Specification Inquiries (e.g. "What documents can I submit?", "Aadhaar requirements")
    elif any(k in msg_lower for k in ["what document", "accepted", "supported", "list", "format", "valid"]):
        reply_paragraphs.append("🏛️ **Supported Identity Documents under Government Guidelines**:\n"
                               "IDShield AI supports 6 primary statutory Indian identity standards and 6 supplementary documents:\n"
                               "1. **Aadhaar Card** (UIDAI 12-digit UID or e-Aadhaar)\n"
                               "2. **PAN Card** (Income Tax 10-char alphanumeric alphanumeric)\n"
                               "3. **Indian Passport** (MEA Type-P 8-character with ICAO MRZ)\n"
                               "4. **Driving Licence** (MoRTH 15/16 character DL format)\n"
                               "5. **Voter ID Card** (ECI 10-character alphanumeric EPIC)\n"
                               "6. **Academic Student ID** (Recognized Colleges/Universities)\n"
                               "7. **Supplementary**: Ration Card, Birth Certificate, Caste Certificate, Domicile Certificate, ABHA Health Card, e-Shram Card.")
        remediation.append("Choose the document that matches your application name and date of birth exactly.")

    # 3. AI Generated / Deepfake / Synthetic Document Questions
    elif any(k in msg_lower for k in ["ai", "fake", "synthetic", "photoshop", "deepfake", "generate", "midjourney"]):
        reply_paragraphs.append("🔍 **AI-Generated Document Detection Forensics**:\n"
                               "IDShield AI applies a multi-layered forensic inspection engine:\n"
                               "• **2D FFT Frequency Spectrum**: Measures high-frequency radial fall-off and deconvolution grid artifacts left by generative diffusion (Midjourney, Stable Diffusion, Flux).\n"
                               "• **Error Level Analysis (ELA)**: Exposes digital splicing and localized resaving differences.\n"
                               "• **Cryptographic QR Validation**: Inspects UIDAI 2048-bit digital signatures and NSDL certificates that cannot be fabricated by AI models.")
        guidelines.append("Under Sections 463 & 465 of the Indian Penal Code and the DPDP Act 2023, submitting fabricated or synthetic identity documents is punishable by law.")
        remediation.append("Always supply legitimate credentials issued by statutory government authorities.")

    # 4. Default / General Inquiries
    else:
        doc_key = doc_type if doc_type in KNOWLEDGE_BASE else "AADHAAR"
        kb_entry = KNOWLEDGE_BASE.get(doc_key, KNOWLEDGE_BASE["AADHAAR"])
        
        reply_paragraphs.append(f"Hello! I am your **IDShield AI Identity Screening Assistant**. I can assist you with document requirements, technical guidelines, and explaining screening outcomes.")
        reply_paragraphs.append(f"**Guidelines for {kb_entry['title']} ({kb_entry['authority']})**:")
        for g in kb_entry["guidelines"]:
            reply_paragraphs.append(f"• {g}")

        remediation.extend(kb_entry["remediation"])
        guidelines.append(f"Issued under the oversight of {kb_entry['authority']}.")

    return ChatResponse(
        reply="\n\n".join(reply_paragraphs),
        remediationSuggestions=remediation,
        relevantGuidelines=guidelines,
        timestamp=datetime.utcnow().isoformat() + "Z"
    )
