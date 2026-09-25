package com.project.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponseDto {
    private String mobileNumber;
    private String message;
    private long ttlSeconds;
    private String debugOtp; // Included in dev mode for UI toast demonstration
}
