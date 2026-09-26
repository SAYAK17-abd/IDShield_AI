package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForensicDetailsDto {
    private Boolean isSynthetic;
    private Double syntheticProbability;
    private Double spectralHighFreqEnergy;
    private Double elaAnomalyScore;
    private Double noiseInconsistencyScore;
    private Boolean qrDetected;
    private String qrSignatureStatus;
    private String qrPayloadPreview;
    private List<String> checksPassed;
    private List<String> checksFlagged;
}
