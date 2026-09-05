package com.project.verification;

import com.project.ai.dto.AiAnalysisResponse;
import com.project.ai.dto.FaceVerificationDto;
import com.project.ai.dto.TamperingDto;
import com.project.verification.dto.RiskScoreDetails;
import com.project.verification.entity.RiskLevel;
import com.project.verification.service.RiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoringServiceTest {

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        riskScoringService = new RiskScoringService();
        ReflectionTestUtils.setField(riskScoringService, "tamperingWeight", 0.30);
        ReflectionTestUtils.setField(riskScoringService, "faceMismatchWeight", 0.30);
        ReflectionTestUtils.setField(riskScoringService, "ocrWeight", 0.20);
        ReflectionTestUtils.setField(riskScoringService, "identityWeight", 0.20);
        ReflectionTestUtils.setField(riskScoringService, "lowRiskMax", 30);
        ReflectionTestUtils.setField(riskScoringService, "mediumRiskMax", 60);
    }

    @Test
    void calculateRisk_CleanDocument_ShouldReturnLowRisk() {
        AiAnalysisResponse cleanResponse = AiAnalysisResponse.builder()
                .tampering(TamperingDto.builder().detected(false).confidence(0.05).build())
                .faceVerification(FaceVerificationDto.builder().matched(true).confidence(0.98).build())
                .inconsistencies(List.of())
                .build();

        RiskScoreDetails details = riskScoringService.calculateRisk(cleanResponse);

        assertEquals(RiskLevel.LOW, details.getRiskLevel());
        assertTrue(details.getRiskScore() <= 30);
        assertFalse(details.getReasons().isEmpty());
    }

    @Test
    void calculateRisk_TamperedAndFaceMismatch_ShouldReturnHighRisk() {
        AiAnalysisResponse badResponse = AiAnalysisResponse.builder()
                .tampering(TamperingDto.builder()
                        .detected(true)
                        .confidence(0.95)
                        .reasons(List.of("Possible photo manipulation"))
                        .build())
                .faceVerification(FaceVerificationDto.builder()
                        .matched(false)
                        .confidence(0.12)
                        .build())
                .inconsistencies(List.of("Date of birth OCR mismatch"))
                .build();

        RiskScoreDetails details = riskScoringService.calculateRisk(badResponse);

        assertEquals(RiskLevel.HIGH, details.getRiskLevel());
        assertTrue(details.getRiskScore() > 60);
        assertTrue(details.getReasons().stream().anyMatch(r -> r.contains("Possible photo manipulation")));
    }
}

