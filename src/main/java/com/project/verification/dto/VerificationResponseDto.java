package com.project.verification.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.dto.OcrDataDto;
import com.project.verification.entity.InvestigationStatus;
import com.project.verification.entity.RiskLevel;
import com.project.verification.entity.VerificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Verification response DTO returned to investigators and dashboard clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {

    private Long id;
    private Long documentId;
    private Long ownerId;
    private String ownerEmail;
    private String documentType;
    private InvestigationStatus investigationStatus;
    private Integer riskScore;
    private RiskLevel riskLevel;

    private Boolean tamperingDetected;
    private Double tamperingConfidence;
    private Boolean faceMatched;
    private Double faceMatchConfidence;

    private OcrDataDto ocrData;
    private List<String> inconsistencies;
    private List<String> reasons;

    private Long reviewedByUserId;
    private String reviewedByUserEmail;
    private String investigatorNotes;

    private Instant createdAt;
    private Instant updatedAt;

    public static VerificationResponseDto fromEntity(VerificationResult v, ObjectMapper objectMapper) {
        OcrDataDto ocr = null;
        List<String> inconsistencies = Collections.emptyList();
        List<String> reasons = Collections.emptyList();

        try {
            if (v.getOcrDataJson() != null) {
                ocr = objectMapper.readValue(v.getOcrDataJson(), OcrDataDto.class);
            }
            if (v.getInconsistenciesJson() != null) {
                inconsistencies = objectMapper.readValue(v.getInconsistenciesJson(), new TypeReference<List<String>>() {});
            }
            if (v.getReasonsJson() != null) {
                reasons = objectMapper.readValue(v.getReasonsJson(), new TypeReference<List<String>>() {});
            }
        } catch (Exception ignored) {
        }

        return VerificationResponseDto.builder()
                .id(v.getId())
                .documentId(v.getDocument().getId())
                .ownerId(v.getDocument().getOwner().getId())
                .ownerEmail(v.getDocument().getOwner().getEmail())
                .documentType(v.getDocument().getDocumentType())
                .investigationStatus(v.getInvestigationStatus())
                .riskScore(v.getRiskScore())
                .riskLevel(v.getRiskLevel())
                .tamperingDetected(v.getTamperingDetected())
                .tamperingConfidence(v.getTamperingConfidence())
                .faceMatched(v.getFaceMatched())
                .faceMatchConfidence(v.getFaceMatchConfidence())
                .ocrData(ocr)
                .inconsistencies(inconsistencies)
                .reasons(reasons)
                .reviewedByUserId(v.getReviewedBy() != null ? v.getReviewedBy().getId() : null)
                .reviewedByUserEmail(v.getReviewedBy() != null ? v.getReviewedBy().getEmail() : null)
                .investigatorNotes(v.getInvestigatorNotes())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}

