import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router
from app.models.face_model import init_face_app
from app.models.ocr_model import init_ocr_engine

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s]: %(message)s"
)
logger = logging.getLogger("ai_service.main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager.
    Pre-loads heavy deep-learning models into memory at server boot.
    Models are preserved across all HTTP requests for high performance.
    """
    logger.info("Starting IDShield AI Service (FastAPI)...")
    try:
        # Preload InsightFace ArcFace
        init_face_app(name="buffalo_sc", det_size=(640, 640))
        # Preload RapidOCR PaddleOCR ONNX
        init_ocr_engine()
        logger.info("All AI neural models warmed up and ready to serve requests.")
    except Exception as ex:
        logger.error("Failed to preload AI models at startup: %s", str(ex), exc_info=True)
        raise ex

    yield

    logger.info("Shutting down IDShield AI Service...")


app = FastAPI(
    title="IDShield AI Screening Service",
    description="Microservice providing real biometric face verification (InsightFace), OCR (RapidOCR), and image tampering forensics for SIH26188.",
    version="1.0.0",
    lifespan=lifespan
)

import os
# Restrict CORS to internal Spring Boot backend and authorized origins (no wildcard in production)
allowed_origins_raw = os.getenv(
    "AI_CORS_ALLOWED_ORIGINS",
    "http://localhost:8080,http://127.0.0.1:8080,http://localhost:3000,http://localhost:5173"
)
allowed_origins = [orig.strip() for orig in allowed_origins_raw.split(",") if orig.strip() and orig.strip() != "*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="127.0.0.1", port=8000, reload=True)
