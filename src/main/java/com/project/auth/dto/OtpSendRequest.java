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
public class OtpSendRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid 10-digit Indian mobile number")
    private String mobileNumber;

    @NotBlank(message = "Captcha ID is required")
    private String captchaId;

    @NotBlank(message = "Captcha answer is required")
    private String captchaAnswer;

    private String purpose; // CITIZEN_LOGIN, CITIZEN_REGISTRATION, OFFICER_LOGIN, OFFICER_REGISTRATION, ADMIN_LOGIN
}
