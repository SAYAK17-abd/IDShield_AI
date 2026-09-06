import base64
import io
import cv2
import numpy as np
from PIL import Image

try:
    import pypdfium2 as pdfium
    PDFIUM_AVAILABLE = True
except ImportError:
    PDFIUM_AVAILABLE = False


def decode_base64_to_bytes(b64_str: str) -> bytes:
    """Safely decodes base64 string, stripping optional data URL header."""
    if not b64_str:
        return b""
    if "," in b64_str:
        b64_str = b64_str.split(",", 1)[1]
    return base64.b64decode(b64_str)


def bytes_to_bgr_image(data: bytes) -> np.ndarray:
    """Converts raw image bytes to OpenCV BGR numpy array."""
    if not data:
        return None
    # If it is a PDF, render first page
    if data.startswith(b"%PDF-"):
        return render_pdf_first_page(data)

    nparr = np.frombuffer(data, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    return img


def render_pdf_first_page(pdf_bytes: bytes) -> np.ndarray:
    """Renders the first page of a PDF file to OpenCV BGR numpy array."""
    if not PDFIUM_AVAILABLE:
        raise RuntimeError("pypdfium2 is required for rendering PDF documents.")

    pdf = pdfium.PdfDocument(pdf_bytes)
    if len(pdf) == 0:
        raise ValueError("PDF document contains no pages.")
    page = pdf[0]
    # Render at 2x resolution (144 dpi) for high OCR and face accuracy
    bitmap = page.render(scale=2.0)
    pil_img = bitmap.to_pil()
    # Convert PIL RGB to OpenCV BGR
    rgb_arr = np.array(pil_img)
    bgr_arr = cv2.cvtColor(rgb_arr, cv2.COLOR_RGB2BGR)
    return bgr_arr


def load_image_from_bytes_or_base64(data_bytes: bytes = None, b64_str: str = None) -> np.ndarray:
    """Loads a BGR image array from either bytes or base64 string."""
    if data_bytes:
        return bytes_to_bgr_image(data_bytes)
    elif b64_str:
        raw = decode_base64_to_bytes(b64_str)
        return bytes_to_bgr_image(raw)
    return None
