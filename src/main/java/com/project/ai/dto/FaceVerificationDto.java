package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerificationDto {
    private Boolean matched;
    private Double confidence; // 0.0 to 1.0
    private String status;     // MATCH, MISMATCH, UNCERTAIN, FACE_NOT_FOUND_DOCUMENT, etc.
    private Double similarity; // raw cosine similarity
    private Double threshold;  // operational threshold
    private Boolean documentFaceDetected;
    private Boolean selfieFaceDetected;
    private Integer documentFaceCount;
    private Integer selfieFaceCount;
    private String reason;
}

