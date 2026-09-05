package com.project.auth.service;

import com.project.audit.entity.AuditEventType;
import com.project.audit.service.AuditService;
import com.project.auth.dto.*;
import com.project.auth.entity.RefreshToken;
import com.project.auth.repository.RefreshTokenRepository;
import com.project.exception.ApiException;
import com.project.exception.UnauthorizedException;
import com.project.security.JwtService;
import com.project.user.dto.UserDto;
import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Authentication Service.
 * Implements:
 * - User registration with BCrypt hashing (CRITICAL CODE EXAMPLE 1)
 * - Login with short-lived JWT generation (CRITICAL CODE EXAMPLE 2)
 * - Refresh-token rotation with family revocation on reuse detection (CRITICAL CODE EXAMPLE 7)
 * - Logout and safe current-user retrieval
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Value("${application.security.jwt.access-token-expiration:900000}")
    private long accessTokenExpirationMs;

    @Value("${application.security.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * User registration with password hashing (CRITICAL CODE EXAMPLE 1)
     */
    @Transactional
    public UserDto register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ApiException("Email is already registered", HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
        }

        // Enforce privilege separation: Public registration CANNOT grant ADMIN or INVESTIGATOR roles
        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        auditService.logEvent(
                AuditEventType.REGISTRATION,
                savedUser.getId(),
                savedUser.getEmail(),
                "USER",
                savedUser.getId().toString(),
                "New user registered with role ROLE_USER",
                httpRequest
        );

        return UserDto.fromEntity(savedUser);
    }

    /**
     * User login with JWT and Refresh Token generation (CRITICAL CODE EXAMPLE 2)
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            auditService.logEvent(
                    AuditEventType.LOGIN_FAILURE,
                    null,
                    normalizedEmail,
                    "AUTH",
                    null,
                    "Invalid password or user not found",
                    httpRequest
            );
            throw e;
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String accessToken = jwtService.generateAccessToken(user, user.getId(), user.getRole().name());
        String rawRefreshToken = generateSecureTokenString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        auditService.logEvent(
                AuditEventType.LOGIN_SUCCESS,
                user.getId(),
                user.getEmail(),
                "AUTH",
                user.getId().toString(),
                "User successfully logged in",
                httpRequest
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpirationMs)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Refresh-Token Rotation with Token Family Revocation on Reuse (CRITICAL CODE EXAMPLE 7)
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String rawRefreshToken = request.getRefreshToken();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = storedToken.getUser();

        // REUSE DETECTION: If this token was already revoked or replaced, this may be a token replay attack!
        if (storedToken.isRevoked() || storedToken.getReplacedByTokenHash() != null) {
            log.warn("SECURITY ALERT: Refresh token reuse detected for user [{}]! Revoking entire token family.", user.getEmail());
            refreshTokenRepository.revokeAllUserTokens(user);
            auditService.logEvent(
                    AuditEventType.LOGOUT,
                    user.getId(),
                    user.getEmail(),
                    "AUTH",
                    user.getId().toString(),
                    "SECURITY BREACH: Refresh token reuse detected. All user sessions invalidated.",
                    httpRequest
            );
            throw new UnauthorizedException("Session compromised. Please login again.");
        }

        if (storedToken.isExpired()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        // ROTATION: Revoke the current token and generate a new token pair
        String newRawRefreshToken = generateSecureTokenString();
        String newTokenHash = hashToken(newRawRefreshToken);

        storedToken.setRevoked(true);
        storedToken.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user, user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(accessTokenExpirationMs)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Logout: revokes refresh token session and logs audit trail (Section 10)
     */
    @Transactional
    public void logout(LogoutRequest request, HttpServletRequest httpRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        Long userId = null;
        if (email != null) {
            userRepository.findByEmail(email).ifPresent(u -> {
                if (request != null && request.getRefreshToken() != null) {
                    String tokenHash = hashToken(request.getRefreshToken());
                    refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            });
        }

        auditService.logEvent(
                AuditEventType.LOGOUT,
                userId,
                email,
                "AUTH",
                userId != null ? userId.toString() : "SESSION",
                "User logged out",
                httpRequest
        );
    }

    /**
     * Get Current Authenticated User (Section 11)
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user in context");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User profile not found"));

        return UserDto.fromEntity(user);
    }

    private String generateSecureTokenString() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}

