package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrDataDto {
    private String name;
    private String dateOfBirth;
    private String documentNumber;
    private String expiryDate;
    private Map<String, String> additionalFields;
}

