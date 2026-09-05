package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Validated response payload received from the FastAPI AI service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    private OcrDataDto ocr;

    @Builder.Default
    private TamperingDto tampering = TamperingDto.builder()
            .detected(false)
            .confidence(0.0)
            .reasons(new ArrayList<>())
            .build();

    @Builder.Default
    private FaceVerificationDto faceVerification = FaceVerificationDto.builder()
            .matched(true)
            .confidence(1.0)
            .build();

    @Builder.Default
    private List<String> inconsistencies = new ArrayList<>();
}

