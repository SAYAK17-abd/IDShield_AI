package com.project.auth.controller;

import com.project.auth.dto.*;
import com.project.auth.service.AuthService;
import com.project.common.ApiResponse;
import com.project.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller exposing registration, login, token rotation, logout,
 * and the Multi-Portal Government Authentication Gateway (Citizen, Officer, Admin).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, multi-portal authentication, OTP, token refresh, and profile inspection")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/captcha")
    @Operation(summary = "Generate anti-bot Captcha challenge", description = "Returns a fresh math challenge puzzle and captcha ID (valid for 3 minutes).")
    public ResponseEntity<ApiResponse<CaptchaResponseDto>> getCaptcha() {
        CaptchaResponseDto captcha = authService.generateCaptcha();
        return ResponseEntity.ok(ApiResponse.success(captcha, "Captcha challenge generated"));
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Dispatch verification OTP", description = "Validates captcha challenge and dispatches 6-digit OTP code to mobile number.")
    public ResponseEntity<ApiResponse<OtpSendResponseDto>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        OtpSendResponseDto response = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/citizen/register")
    @Operation(summary = "Citizen Registration", description = "Registers citizen account via verified Mobile OTP and Government ID.")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCitizen(@Valid @RequestBody CitizenRegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.registerCitizen(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Citizen registration successful"));
    }

    @PostMapping("/citizen/login")
    @Operation(summary = "Citizen Passwordless Sign-In", description = "Authenticates citizen via verified Mobile OTP.")
    public ResponseEntity<ApiResponse<AuthResponse>> loginCitizen(@Valid @RequestBody CitizenLoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.loginCitizen(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Citizen sign-in successful"));
    }

    @PostMapping("/officer/register")
    @Operation(summary = "Verification Officer Registration", description = "Registers officer with Employee ID and 2FA OTP.")
    public ResponseEntity<ApiResponse<AuthResponse>> registerOfficer(@Valid @RequestBody OfficerRegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.registerOfficer(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Officer registered successfully"));
    }

    @PostMapping("/officer/login")
    @Operation(summary = "Verification Officer 2FA Login", description = "Authenticates officer with Employee ID, Password, and 2FA OTP.")
    public ResponseEntity<ApiResponse<AuthResponse>> loginOfficer(@Valid @RequestBody OfficerLoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.loginOfficer(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Officer authentication successful"));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "System Administrator 2FA Login", description = "Authenticates administrator with credentials and 2FA OTP.")
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.loginAdmin(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Administrator authentication successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a standard USER account. Cannot escalate privileges.")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        UserDto registeredUser = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registeredUser, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Validates credentials and returns JWT access and refresh token pair.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and returns a new access token. Revokes token family if reuse is detected.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "User logout", description = "Invalidates the refresh token session and logs the audit event.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest httpRequest) {
        authService.logout(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user profile", description = "Returns safe user profile details (id, name, email, role).")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto currentUser = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}
