package com.project.ai;

import com.project.ai.client.AiClient;
import com.project.ai.dto.AiAnalysisRequest;
import com.project.ai.dto.AiAnalysisResponse;
import com.project.ai.dto.FaceVerificationDto;
import com.project.ai.dto.TamperingDto;
import com.project.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AiClientTest {

    private AiClient aiClient;

    @BeforeEach
    void setUp() {
        // Initialize client with custom timeout and mock fallback enabled
        aiClient = new AiClient("http://localhost:8000", "test-secret-key-12345", 5000, 10000, true);
    }

    @Test
    void validateAiResponse_ConfidenceClamping_ShouldClampToBounds() {
        AiAnalysisResponse response = AiAnalysisResponse.builder()
                .tampering(TamperingDto.builder()
                        .detected(true)
                        .confidence(1.5) // Out of bounds (> 1.0)
                        .build())
                .faceVerification(FaceVerificationDto.builder()
                        .matched(false)
                        .confidence(-0.2) // Out of bounds (< 0.0)
                        .build())
                .build();

        // Invoke private validation method via ReflectionTestUtils
        ReflectionTestUtils.invokeMethod(aiClient, "validateAiResponse", response);

        assertEquals(1.0, response.getTampering().getConfidence());
        assertEquals(0.0, response.getFaceVerification().getConfidence());
    }

    @Test
    void analyzeDocument_WhenServiceOfflineAndFallbackEnabled_ShouldReturnMockResponse() {
        AiAnalysisRequest request = AiAnalysisRequest.builder()
                .documentId(101L)
                .documentType("AADHAAR_CARD")
                .originalFilename("aadhaar.jpg")
                .fileBase64("dummyBase64")
                .build();

        // AI service is offline, but fallback is enabled
        AiAnalysisResponse response = aiClient.analyzeDocument(request);

        assertNotNull(response);
        assertNotNull(response.getOcr());
        assertEquals("4920 8192 3841", response.getOcr().getDocumentNumber());
        assertNotNull(response.getInconsistencies());
        assertFalse(response.getFaceVerification().getMatched());
    }

    @Test
    void analyzeDocument_WhenServiceOfflineAndFallbackDisabled_ShouldThrowAiServiceException() {
        AiClient strictClient = new AiClient("http://localhost:8000", "test-secret-key-12345", 1000, 1000, false);

        AiAnalysisRequest request = AiAnalysisRequest.builder()
                .documentId(102L)
                .documentType("PAN_CARD")
                .originalFilename("pan.jpg")
                .fileBase64("dummyBase64")
                .build();

        assertThrows(AiServiceException.class, () -> strictClient.analyzeDocument(request));
    }
}

