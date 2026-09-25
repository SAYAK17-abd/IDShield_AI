package com.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "2FA OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
    private String otpCode;
}
