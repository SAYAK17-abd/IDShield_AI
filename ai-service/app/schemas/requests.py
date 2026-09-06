from typing import Optional
from pydantic import BaseModel, Field


class AiAnalysisRequest(BaseModel):
    documentId: Optional[int] = None
    documentType: Optional[str] = "IDENTITY_CARD"
    originalFilename: Optional[str] = None
    mimeType: Optional[str] = None
    fileBase64: Optional[str] = Field(None, description="Base64 encoded document image or PDF")
    selfieBase64: Optional[str] = Field(None, description="Base64 encoded reference selfie image")
    selfieFilename: Optional[str] = None
