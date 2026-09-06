import cv2
import numpy as np
from typing import Optional, List
from app.schemas.responses import ImageQualityDto


def assess_image_quality(img: Optional[np.ndarray]) -> ImageQualityDto:
    """
    Computes objective image quality indicators:
    - Laplacian blur variance
    - Mean luminance (brightness)
    - Contrast (standard deviation of luminance)
    - Spatial resolution
    """
    if img is None:
        return ImageQualityDto(
            score=0.0,
            blur=0.0,
            brightness=0.0,
            resolution="POOR",
            issues=["Image buffer is empty or corrupt"]
        )

    h, w = img.shape[:2]
    issues: List[str] = []

    # 1. Resolution Check
    resolution = "GOOD"
    if w < 300 or h < 300:
        resolution = "LOW"
        issues.append("Image resolution is below 300x300 pixels")
    elif w < 600 or h < 400:
        resolution = "MEDIUM"

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # 2. Blur Check via Laplacian Variance
    lap_var = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    blur_score = lap_var
    if lap_var < 80.0:
        issues.append("Image is severely blurry; details may be degraded")
    elif lap_var < 150.0:
        issues.append("Mild image motion or focus blur detected")

    # 3. Brightness & Contrast
    mean_brightness = float(np.mean(gray))
    std_contrast = float(np.std(gray))

    if mean_brightness < 40.0:
        issues.append("Image is underexposed / too dark")
    elif mean_brightness > 230.0:
        issues.append("Image is overexposed / washed out")

    if std_contrast < 30.0:
        issues.append("Low contrast detected between text and background")

    # Calculate aggregate quality score [0.0, 1.0]
    score = 1.0
    if resolution == "LOW":
        score -= 0.30
    elif resolution == "MEDIUM":
        score -= 0.10

    if lap_var < 80.0:
        score -= 0.35
    elif lap_var < 150.0:
        score -= 0.15

    if mean_brightness < 40.0 or mean_brightness > 230.0:
        score -= 0.20

    if std_contrast < 30.0:
        score -= 0.15

    score = float(np.clip(score, 0.05, 1.0))

    return ImageQualityDto(
        score=round(score, 3),
        blur=round(blur_score, 2),
        brightness=round(mean_brightness, 2),
        resolution=resolution,
        issues=issues
    )
