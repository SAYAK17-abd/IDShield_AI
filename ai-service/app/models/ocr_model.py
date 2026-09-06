import logging
from typing import Optional
from rapidocr_onnxruntime import RapidOCR

logger = logging.getLogger("ai_service.models.ocr")

_ocr_engine: Optional[RapidOCR] = None


def get_ocr_engine() -> RapidOCR:
    """Returns the singleton instance of RapidOCR (PaddleOCR ONNX engine)."""
    global _ocr_engine
    if _ocr_engine is None:
        init_ocr_engine()
    return _ocr_engine


def init_ocr_engine():
    """Initializes the ONNX-based PaddleOCR engine."""
    global _ocr_engine
    logger.info("Initializing RapidOCR (PaddleOCR ONNX) engine...")
    _ocr_engine = RapidOCR()
    logger.info("RapidOCR engine successfully loaded and ready.")
