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
public class OfficerLoginRequest {

    @NotBlank(message = "Employee ID or registered mobile is required")
    private String employeeIdOrMobile;

    @NotBlank(message = "Password is required")
    private String password;

    private String captchaId;
    private String captchaAnswer;

    private String otpCode;
}
