import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app, raise_server_exceptions=False)
VALID_KEY = "dev-internal-ai-key-sih26188"


def test_health_endpoint_public():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json().get("status") == "UP"
    print("[TEST PASSED] /health endpoint is accessible")


def test_analyze_without_service_key_returns_401():
    response = client.post("/ai/analyze", json={"fileBase64": "dummy"})
    assert response.status_code == 401
    assert "Unauthorized" in response.json().get("detail", "")
    print("[TEST PASSED] /ai/analyze without X-AI-Service-Key is rejected with 401")


def test_analyze_with_invalid_service_key_returns_401():
    headers = {"X-AI-Service-Key": "completely-invalid-attacker-key"}
    response = client.post("/ai/analyze", json={"fileBase64": "dummy"}, headers=headers)
    assert response.status_code == 401
    assert "Unauthorized" in response.json().get("detail", "")
    print("[TEST PASSED] /ai/analyze with wrong X-AI-Service-Key is rejected with 401")


def test_analyze_with_valid_service_key_passes_auth():
    headers = {"X-AI-Service-Key": VALID_KEY}
    # Send empty payload to check that authentication succeeds and validation continues
    response = client.post("/ai/analyze", json={}, headers=headers)
    # Auth passed! The endpoint processes the request and responds with 400 Bad Request because fileBase64 is missing
    assert response.status_code == 400
    assert "fileBase64" in response.json().get("detail", "")
    print("[TEST PASSED] /ai/analyze with valid X-AI-Service-Key passes authentication successfully")


if __name__ == "__main__":
    print("Running FastAPI Service Authentication Tests...")
    test_health_endpoint_public()
    test_analyze_without_service_key_returns_401()
    test_analyze_with_invalid_service_key_returns_401()
    test_analyze_with_valid_service_key_passes_auth()
    print("ALL FASTAPI AUTHENTICATION TESTS PASSED!")

