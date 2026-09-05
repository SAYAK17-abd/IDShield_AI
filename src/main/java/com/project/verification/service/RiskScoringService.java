package com.project.verification.service;

import com.project.ai.dto.AiAnalysisResponse;
import com.project.verification.dto.RiskScoreDetails;
import com.project.verification.entity.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Transparent Risk-Scoring Engine.
 * (CRITICAL CODE EXAMPLE 10)
 *
 * Weighting Formula:
 * - Tampering Signal: 30%
 * - Face Mismatch: 30%
 * - OCR Inconsistencies: 20%
 * - Identity/Document Inconsistencies: 20%
 *
 * Thresholds:
 * - 0–30  -> LOW Risk
 * - 31–60 -> MEDIUM Risk
 * - 61–100 -> HIGH Risk
 *
 * Emits discrete explanatory reasons for investigator review without claiming definitive fraud.
 */
@Slf4j
@Service
public class RiskScoringService {

    @Value("${application.risk-scoring.weights.tampering:0.30}")
    private double tamperingWeight;

    @Value("${application.risk-scoring.weights.face-mismatch:0.30}")
    private double faceMismatchWeight;

    @Value("${application.risk-scoring.weights.ocr-inconsistency:0.20}")
    private double ocrWeight;

    @Value("${application.risk-scoring.weights.identity-inconsistency:0.20}")
    private double identityWeight;

    @Value("${application.risk-scoring.thresholds.low-max:30}")
    private int lowRiskMax;

    @Value("${application.risk-scoring.thresholds.medium-max:60}")
    private int mediumRiskMax;

    public RiskScoreDetails calculateRisk(AiAnalysisResponse aiResponse) {
        List<String> reasons = new ArrayList<>();

        // 1. Tampering Component (Weight: 30%)
        double tamperingRaw = 0.0;
        if (aiResponse.getTampering() != null) {
            Double conf = aiResponse.getTampering().getConfidence();
            boolean detected = Boolean.TRUE.equals(aiResponse.getTampering().getDetected());

            if (detected) {
                tamperingRaw = (conf != null) ? conf * 100.0 : 80.0;
                if (aiResponse.getTampering().getReasons() != null && !aiResponse.getTampering().getReasons().isEmpty()) {
                    reasons.addAll(aiResponse.getTampering().getReasons());
                } else {
                    reasons.add(String.format("Potential image or layout tampering detected (Confidence: %.0f%%)", tamperingRaw));
                }
            } else if (conf != null && conf > 0.40) {
                tamperingRaw = conf * 50.0;
                reasons.add(String.format("Low-confidence artifact anomalies detected (Confidence: %.0f%%)", conf * 100.0));
            }
        }
        double tamperingScore = Math.min(100.0, tamperingRaw) * tamperingWeight;

        // 2. Face Mismatch Component (Weight: 30%)
        double faceMismatchRaw = 0.0;
        if (aiResponse.getFaceVerification() != null) {
            boolean matched = Boolean.TRUE.equals(aiResponse.getFaceVerification().getMatched());
            Double conf = aiResponse.getFaceVerification().getConfidence();

            if (!matched) {
                // If face does not match, higher face mismatch risk
                faceMismatchRaw = (conf != null) ? (1.0 - conf) * 100.0 : 85.0;
                faceMismatchRaw = Math.max(70.0, faceMismatchRaw);
                reasons.add(String.format("Portrait does not match reference photo (Match confidence: %.0f%%)",
                        (conf != null ? conf * 100.0 : 0.0)));
            } else if (conf != null && conf < 0.65) {
                faceMismatchRaw = (1.0 - conf) * 60.0;
                reasons.add(String.format("Facial similarity confidence is low (%.0f%%)", conf * 100.0));
            }
        }
        double faceMismatchScore = Math.min(100.0, faceMismatchRaw) * faceMismatchWeight;

        // 3. OCR & Inconsistency Component (Weight: 20% OCR, 20% Identity)
        double ocrRaw = 0.0;
        double identityRaw = 0.0;

        if (aiResponse.getInconsistencies() != null && !aiResponse.getInconsistencies().isEmpty()) {
            for (String inc : aiResponse.getInconsistencies()) {
                reasons.add(inc);
                if (inc.toLowerCase().contains("ocr") || inc.toLowerCase().contains("font") || inc.toLowerCase().contains("text")) {
                    ocrRaw += 40.0;
                } else {
                    identityRaw += 40.0;
                }
            }
        }

        double ocrScore = Math.min(100.0, ocrRaw) * ocrWeight;
        double identityScore = Math.min(100.0, identityRaw) * identityWeight;

        // Aggregate total score bounded between 0 and 100
        int totalScore = (int) Math.round(Math.min(100.0, tamperingScore + faceMismatchScore + ocrScore + identityScore));

        // Classify Risk Level
        RiskLevel level;
        if (totalScore <= lowRiskMax) {
            level = RiskLevel.LOW;
        } else if (totalScore <= mediumRiskMax) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.HIGH;
        }

        if (reasons.isEmpty()) {
            reasons.add("No significant anomalies or tampering detected across inspection signals");
        }

        log.info("Risk calculation complete: Total Score [{}/100], Level [{}], Signals: Tampering={:.1f}, Face={:.1f}, OCR={:.1f}, Identity={:.1f}",
                totalScore, level, tamperingScore, faceMismatchScore, ocrScore, identityScore);

        return RiskScoreDetails.builder()
                .riskScore(totalScore)
                .riskLevel(level)
                .reasons(reasons)
                .tamperingScore(tamperingScore)
                .faceMismatchScore(faceMismatchScore)
                .ocrInconsistencyScore(ocrScore)
                .identityInconsistencyScore(identityScore)
                .build();
    }
}

