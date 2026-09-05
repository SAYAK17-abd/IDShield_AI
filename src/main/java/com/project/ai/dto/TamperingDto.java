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
public class TamperingDto {
    private Boolean detected;
    private Double confidence; // 0.0 to 1.0
    private List<String> reasons;
}

