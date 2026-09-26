from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class OcrDataDto(BaseModel):
    name: Optional[str] = None
    dateOfBirth: Optional[str] = None
    documentNumber: Optional[str] = None
    expiryDate: Optional[str] = None
    additionalFields: Optional[Dict[str, str]] = None
    rawText: Optional[str] = None
    confidence: Optional[float] = None


class FaceVerificationDto(BaseModel):
    matched: bool = False
    confidence: float = 0.0
    status: str = "MISMATCH"
    similarity: Optional[float] = None
    threshold: Optional[float] = 0.45
    documentFaceDetected: bool = False
    selfieFaceDetected: bool = False
    documentFaceCount: int = 0
    selfieFaceCount: int = 0
    reason: Optional[str] = None


class ForensicDetailsDto(BaseModel):
    isSynthetic: bool = False
    syntheticProbability: float = 0.0
    spectralHighFreqEnergy: float = 0.0
    elaAnomalyScore: float = 0.0
    noiseInconsistencyScore: float = 0.0
    qrDetected: bool = False
    qrSignatureStatus: str = "NOT_CHECKED"  # VERIFIED_UIDAI_STRUCTURE, VERIFIED_NSDL_FORMAT, PLAIN_DATA, INVALID_OR_FAKE, NOT_FOUND
    qrPayloadPreview: Optional[str] = None
    checksPassed: List[str] = Field(default_factory=list)
    checksFlagged: List[str] = Field(default_factory=list)


class TamperingDto(BaseModel):
    detected: bool = False
    confidence: float = 0.0
    reasons: List[str] = Field(default_factory=list)
    status: str = "CLEAN"
    isSynthetic: bool = False
    syntheticProbability: float = 0.0
    forensicDetails: Optional[ForensicDetailsDto] = None


class ImageQualityDto(BaseModel):
    score: float = 1.0
    blur: float = 0.0
    brightness: float = 0.0
    resolution: str = "GOOD"
    issues: List[str] = Field(default_factory=list)


class RiskIndicatorDto(BaseModel):
    type: str
    severity: str  # HIGH, MEDIUM, LOW
    message: str


class ProcessingMetaDto(BaseModel):
    processingTimeMs: int = 0
    modelVersion: str = "InsightFace-ArcFace+RapidOCR-v1+ForensicAI"


class AiAnalysisResponse(BaseModel):
    status: str = "COMPLETED"
    ocr: Optional[OcrDataDto] = None
    tampering: TamperingDto = Field(default_factory=TamperingDto)
    faceVerification: FaceVerificationDto = Field(default_factory=FaceVerificationDto)
    imageQuality: Optional[ImageQualityDto] = None
    inconsistencies: List[str] = Field(default_factory=list)
    riskIndicators: List[RiskIndicatorDto] = Field(default_factory=list)
    processing: Optional[ProcessingMetaDto] = None


class ChatResponse(BaseModel):
    reply: str
    remediationSuggestions: List[str] = Field(default_factory=list)
    relevantGuidelines: List[str] = Field(default_factory=list)
    timestamp: Optional[str] = None
