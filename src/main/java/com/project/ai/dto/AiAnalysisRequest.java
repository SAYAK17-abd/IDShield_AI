package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisRequest {
    private Long documentId;
    private String documentType;
    private String fileBase64;
    private String originalFilename;
    private String mimeType;
    private String selfieBase64;
    private String selfieFilename;
    private String selfieMimeType;
}

