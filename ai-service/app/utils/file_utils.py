import os
import tempfile
from typing import Generator
from contextlib import contextmanager

MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024  # 10 MB

ALLOWED_IMAGE_MIMES = {
    "image/jpeg", "image/png", "image/webp", "application/pdf"
}


def validate_file_size(data_bytes: bytes, max_bytes: int = MAX_FILE_SIZE_BYTES):
    if len(data_bytes) > max_bytes:
        raise ValueError(f"File size exceeds maximum allowed limit of {max_bytes // (1024 * 1024)}MB")


@contextmanager
def temporary_file(suffix: str = ".tmp") -> Generator[str, None, None]:
    """Securely creates a temp file and ensures deletion upon exit."""
    fd, path = tempfile.mkstemp(suffix=suffix)
    os.close(fd)
    try:
        yield path
    finally:
        if os.path.exists(path):
            try:
                os.remove(path)
            except OSError:
                pass
