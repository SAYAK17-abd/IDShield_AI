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
public class CitizenRegisterRequest {

    @NotBlank(message = "Full Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid 10-digit Indian mobile number")
    private String mobileNumber;

    @NotBlank(message = "Date of Birth is required")
    private String dob; // YYYY-MM-DD

    @NotBlank(message = "Government ID Type is required")
    private String govtIdType; // AADHAAR, PAN, VOTER_ID, PASSPORT

    @NotBlank(message = "Government ID Number is required")
    private String govtIdNumber;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
    private String otpCode;
}
