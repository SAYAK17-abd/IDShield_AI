import io
import cv2
import numpy as np
from PIL import Image, ImageChops, ImageEnhance
from typing import Optional, List, Tuple
from app.schemas.responses import TamperingDto


def calculate_ela(img_bgr: np.ndarray, quality: int = 90) -> Tuple[float, np.ndarray]:
    """
    Computes Error Level Analysis (ELA).
    Saves image to JPEG at 90% quality and computes absolute difference.
    Returns the mean error level and high-error percentile.
    """
    # Convert OpenCV BGR to PIL Image
    rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    original_pil = Image.fromarray(rgb)

    buffer = io.BytesIO()
    original_pil.save(buffer, format="JPEG", quality=quality)
    buffer.seek(0)
    resaved_pil = Image.open(buffer)

    # Compute difference
    ela_image = ImageChops.difference(original_pil, resaved_pil)
    extrema = ela_image.getextrema()
    max_diff = max([ex[1] for ex in extrema])
    scale = 255.0 / max(1, max_diff)
    ela_image = ImageEnhance.Brightness(ela_image).enhance(scale)

    ela_arr = np.array(ela_image)
    mean_ela = float(np.mean(ela_arr))
    p95_ela = float(np.percentile(ela_arr, 95))

    return mean_ela, p95_ela


def detect_noise_inconsistency(img_bgr: np.ndarray) -> Tuple[float, bool]:
    """
    Analyzes local noise variance across 16 grid tiles.
    Inconsistent local noise indicates potential image splicing or digital insertion.
    """
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    denoised = cv2.medianBlur(gray, 3)
    noise_residual = cv2.absdiff(gray, denoised)

    h, w = gray.shape
    tile_h, tile_w = max(10, h // 4), max(10, w // 4)
    tile_variances = []

    for i in range(4):
        for j in range(4):
            tile = noise_residual[i * tile_h:(i + 1) * tile_h, j * tile_w:(j + 1) * tile_w]
            if tile.size > 0:
                tile_variances.append(float(np.var(tile)))

    if not tile_variances:
        return 0.0, False

    var_of_variances = float(np.var(tile_variances))
    # If the variance across tiles is abnormally high, noise is non-uniform
    is_inconsistent = var_of_variances > 120.0
    return var_of_variances, is_inconsistent


def detect_tampering(img_bgr: Optional[np.ndarray]) -> TamperingDto:
    """
    Executes multi-signal document forensics:
    1. Error Level Analysis (ELA) for compression discrepancies
    2. Local tile noise variance for digital cut-and-paste splicing
    """
    if img_bgr is None:
        return TamperingDto(
            detected=False,
            confidence=0.0,
            reasons=["Image unavailable for tamper inspection"],
            status="CLEAN"
        )

    reasons: List[str] = []
    tamper_score = 0.0

    # 1. ELA Inspection
    mean_ela, p95_ela = calculate_ela(img_bgr, quality=90)
    if p95_ela > 210.0 and mean_ela > 45.0:
        tamper_score += 0.45
        reasons.append("Elevated compression rate anomaly detected across local image regions (ELA)")
    elif p95_ela > 180.0:
        tamper_score += 0.20
        reasons.append("Mild local compression discrepancy detected (ELA)")

    # 2. Noise Inconsistency Inspection
    noise_var, noise_inconsistent = detect_noise_inconsistency(img_bgr)
    if noise_inconsistent:
        tamper_score += 0.35
        reasons.append("Non-uniform sensor noise profile detected across document tiles (possible splicing)")

    tamper_score = float(np.clip(tamper_score, 0.05, 0.95))
    detected = tamper_score >= 0.50

    status = "SUSPECTED" if detected else "CLEAN"

    return TamperingDto(
        detected=detected,
        confidence=round(tamper_score, 3),
        reasons=reasons,
        status=status
    )
