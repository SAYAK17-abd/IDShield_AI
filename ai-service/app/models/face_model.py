import logging
from typing import Optional
from insightface.app import FaceAnalysis

logger = logging.getLogger("ai_service.models.face")

_face_app: Optional[FaceAnalysis] = None


def get_face_app() -> FaceAnalysis:
    """Returns the singleton instance of InsightFace FaceAnalysis."""
    global _face_app
    if _face_app is None:
        init_face_app()
    return _face_app


def init_face_app(name: str = "buffalo_sc", det_size: tuple = (640, 640)):
    """Initializes and pre-warms the InsightFace detector and recognizer."""
    global _face_app
    logger.info("Initializing InsightFace model [%s] with det_size=%s...", name, det_size)
    app = FaceAnalysis(name=name, providers=["CPUExecutionProvider"])
    app.prepare(ctx_id=0, det_size=det_size)
    _face_app = app
    logger.info("InsightFace model [%s] successfully loaded and ready.", name)
