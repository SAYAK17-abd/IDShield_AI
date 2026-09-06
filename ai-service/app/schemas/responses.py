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


class TamperingDto(BaseModel):
    detected: bool = False
    confidence: float = 0.0
    reasons: List[str] = Field(default_factory=list)
    status: str = "CLEAN"


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
    modelVersion: str = "InsightFace-ArcFace+RapidOCR-v1"


class AiAnalysisResponse(BaseModel):
    status: str = "COMPLETED"
    ocr: Optional[OcrDataDto] = None
    tampering: TamperingDto = Field(default_factory=TamperingDto)
    faceVerification: FaceVerificationDto = Field(default_factory=FaceVerificationDto)
    imageQuality: Optional[ImageQualityDto] = None
    inconsistencies: List[str] = Field(default_factory=list)
    riskIndicators: List[RiskIndicatorDto] = Field(default_factory=list)
    processing: Optional[ProcessingMetaDto] = None
