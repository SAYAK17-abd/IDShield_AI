package com.project.verification.dto;

import com.project.verification.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Detailed breakdown of calculated risk score and transparent contributing signals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreDetails {
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> reasons;
    private double tamperingScore;
    private double faceMismatchScore;
    private double ocrInconsistencyScore;
    private double identityInconsistencyScore;
}

