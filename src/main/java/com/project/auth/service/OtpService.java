package com.project.auth.service;

import com.project.auth.dto.OtpSendResponseDto;
import com.project.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise OTP generation, dispatch simulation, and verification engine.
 */
@Service
@Slf4j
public class OtpService {

    private static final long OTP_TTL_SECONDS = 300; // 5 minutes
    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final Map<String, RequestLimiter> requestRateLimits = new ConcurrentHashMap<>();

    private record OtpEntry(String code, Instant expiresAt, AtomicInteger failedAttempts) {}
    private record RequestLimiter(AtomicInteger count, Instant resetTime) {}

    public OtpSendResponseDto generateAndSendOtp(String mobileNumber, String purpose) {
        String cleanedMobile = sanitizeMobile(mobileNumber);
        checkRateLimit(cleanedMobile);

        // Clean expired
        cleanExpired();

        String otp = String.format("%06d", random.nextInt(1_000_000));
        String cacheKey = buildKey(cleanedMobile, purpose);

        otpCache.put(cacheKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_TTL_SECONDS), new AtomicInteger(0)));

        // Prominent forensic/development log
        log.info("====================================================================");
        log.info("  [SIMULATED SMS GATEWAY] Direct Dispatch to: +91-{}", cleanedMobile);
        log.info("  Purpose: {}", purpose != null ? purpose : "IDENTITY_AUTHENTICATION");
        log.info("  >> ONE-TIME PASSWORD: [{}] <<", otp);
        log.info("  Valid for 5 minutes. Digital India / UIDAI Zero-Trust Standard");
        log.info("====================================================================");

        boolean isDev = activeProfile != null && (activeProfile.contains("dev") || activeProfile.contains("postgres") || activeProfile.contains("test"));
        String debugCode = isDev ? otp : null;

        return OtpSendResponseDto.builder()
                .mobileNumber(cleanedMobile)
                .message("One-time password dispatched successfully to +91-" + maskMobile(cleanedMobile))
                .ttlSeconds(OTP_TTL_SECONDS)
                .debugOtp(debugCode)
                .build();
    }

    public boolean verifyOtp(String mobileNumber, String otpCode, String purpose) {
        if (mobileNumber == null || otpCode == null) {
            return false;
        }

        String cleanedMobile = sanitizeMobile(mobileNumber);
        String cacheKey = buildKey(cleanedMobile, purpose);

        OtpEntry entry = otpCache.get(cacheKey);
        if (entry == null) {
            log.warn("OTP verification failed: No active OTP found for mobile [{}] with purpose [{}]", cleanedMobile, purpose);
            return false;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            otpCache.remove(cacheKey);
            log.warn("OTP verification failed: Expired code for mobile [{}]", cleanedMobile);
            return false;
        }

        if (entry.failedAttempts().get() >= MAX_FAILED_ATTEMPTS) {
            otpCache.remove(cacheKey);
            log.warn("OTP revoked due to exceeding max failed attempts: mobile [{}]", cleanedMobile);
            throw new ValidationException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        boolean matched = entry.code().equals(otpCode.trim());
        if (matched) {
            otpCache.remove(cacheKey); // Single-use consumption
            log.info("OTP verified successfully for mobile [{}] under purpose [{}]", cleanedMobile, purpose);
            return true;
        } else {
            int attempts = entry.failedAttempts().incrementAndGet();
            log.warn("Incorrect OTP for mobile [{}] (Attempt {}/{})", cleanedMobile, attempts, MAX_FAILED_ATTEMPTS);
            return false;
        }
    }

    private void checkRateLimit(String mobile) {
        Instant now = Instant.now();
        RequestLimiter limiter = requestRateLimits.compute(mobile, (k, existing) -> {
            if (existing == null || now.isAfter(existing.resetTime())) {
                return new RequestLimiter(new AtomicInteger(1), now.plusSeconds(600)); // 10 minutes window
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (limiter.count().get() > 5) {
            throw new ValidationException("Too many OTP requests. Please wait a few minutes before trying again.");
        }
    }

    private String sanitizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            throw new ValidationException("Mobile number is required");
        }
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        if (digits.length() != 10) {
            throw new ValidationException("Please provide a valid 10-digit Indian mobile number");
        }
        return digits;
    }

    private String maskMobile(String mobile) {
        if (mobile.length() != 10) return mobile;
        return mobile.substring(0, 2) + "******" + mobile.substring(8);
    }

    private String buildKey(String mobile, String purpose) {
        return mobile + ":" + (purpose != null ? purpose.toUpperCase() : "GENERAL");
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        otpCache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
        requestRateLimits.entrySet().removeIf(e -> now.isAfter(e.getValue().resetTime()));
    }
}
