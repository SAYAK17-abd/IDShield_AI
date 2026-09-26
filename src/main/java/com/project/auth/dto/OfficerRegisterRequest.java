package com.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerRegisterRequest {

    @NotBlank(message = "Officer Full Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Employee ID is required")
    @Pattern(regexp = "^[A-Z0-9-]{4,20}$", message = "Invalid Employee ID format (e.g., OFF-100234)")
    private String employeeId;

    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid 10-digit Indian mobile number")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String department;

    private String captchaId;
    private String captchaAnswer;

    private String otpCode;
}
