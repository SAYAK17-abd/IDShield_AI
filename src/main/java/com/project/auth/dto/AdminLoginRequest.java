package com.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequest {

    @NotBlank(message = "Admin email, employee ID, or mobile is required")
    private String adminIdentifier;

    @NotBlank(message = "Password is required")
    private String password;

    private String captchaId;
    private String captchaAnswer;

    private String otpCode;
}
