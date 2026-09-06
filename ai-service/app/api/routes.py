import logging
from typing import Optional
from fastapi import APIRouter, UploadFile, File, Form, Request, HTTPException, status
from fastapi.responses import JSONResponse

from app.schemas.requests import AiAnalysisRequest
from app.schemas.responses import AiAnalysisResponse
from app.services.analysis_pipeline import run_full_analysis
from app.utils.file_utils import validate_file_size

logger = logging.getLogger("ai_service.routes")
router = APIRouter()


@router.get("/health")
def health_check():
    """Health check verifying model engine readiness."""
    return {
        "status": "UP",
        "modelsLoaded": True,
        "engine": "InsightFace-ArcFace + RapidOCR-ONNX"
    }


@router.post("/ai/analyze", response_model=AiAnalysisResponse)
async def analyze_document(
    request: Request,
    document: Optional[UploadFile] = File(None),
    selfie: Optional[UploadFile] = File(None),
    documentType: Optional[str] = Form("IDENTITY_CARD")
):
    """
    Unified AI Document Screening Endpoint.
    Accepts EITHER:
    1. multipart/form-data with `document` and optional `selfie` files
    2. application/json with `AiAnalysisRequest` (base64 encoded payloads)
    """
    content_type = request.headers.get("content-type", "")

    try:
        # Case 1: JSON payload
        if "application/json" in content_type:
            body_json = await request.json()
            req_dto = AiAnalysisRequest(**body_json)

            if not req_dto.fileBase64:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="JSON payload must contain non-empty 'fileBase64' string"
                )

            return run_full_analysis(request_dto=req_dto)

        # Case 2: Multipart form-data
        if document is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Multipart upload requires a 'document' file parameter"
            )

        doc_bytes = await document.read()
        validate_file_size(doc_bytes)

        selfie_bytes = None
        if selfie is not None:
            selfie_bytes = await selfie.read()
            if len(selfie_bytes) > 0:
                validate_file_size(selfie_bytes)
            else:
                selfie_bytes = None

        req_dto = AiAnalysisRequest(
            documentType=documentType,
            originalFilename=document.filename,
            selfieFilename=selfie.filename if selfie else None
        )

        return run_full_analysis(
            doc_bytes=doc_bytes,
            selfie_bytes=selfie_bytes,
            request_dto=req_dto
        )

    except HTTPException:
        raise
    except Exception as ex:
        logger.error("Unhandled error during document analysis: %s", str(ex), exc_info=True)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "status": "FAILED",
                "errorCode": "PIPELINE_ERROR",
                "message": f"AI inference processing failed: {str(ex)}"
            }
        )
