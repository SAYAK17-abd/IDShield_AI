package com.project.auth.service;

import com.project.auth.dto.CaptchaResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory anti-bot Captcha challenge generation and validation service.
 */
@Service
@Slf4j
public class CaptchaService {

    private static final long CAPTCHA_TTL_SECONDS = 180; // 3 minutes
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaRecord> cache = new ConcurrentHashMap<>();

    private record CaptchaRecord(String expectedAnswer, Instant expiresAt) {}

    public CaptchaResponseDto generateCaptcha() {
        cleanExpiredEntries();

        int a = random.nextInt(20) + 1;
        int b = random.nextInt(15) + 1;
        boolean isAddition = random.nextBoolean();

        String question;
        int answer;
        if (isAddition) {
            question = a + " + " + b + " = ?";
            answer = a + b;
        } else {
            int max = Math.max(a, b);
            int min = Math.min(a, b);
            question = max + " - " + min + " = ?";
            answer = max - min;
        }

        String captchaId = UUID.randomUUID().toString();
        cache.put(captchaId, new CaptchaRecord(String.valueOf(answer), Instant.now().plusSeconds(CAPTCHA_TTL_SECONDS)));

        log.debug("Generated Captcha [{}]: question='{}', answer='{}'", captchaId, question, answer);
        return CaptchaResponseDto.builder()
                .captchaId(captchaId)
                .question(question)
                .ttlSeconds(CAPTCHA_TTL_SECONDS)
                .build();
    }

    public boolean validateCaptcha(String captchaId, String userAnswer) {
        if (captchaId == null || captchaId.isBlank() || userAnswer == null || userAnswer.isBlank()) {
            return false;
        }

        CaptchaRecord record = cache.remove(captchaId); // Single-use consumption
        if (record == null) {
            log.warn("Captcha ID not found or already consumed: [{}]", captchaId);
            return false;
        }

        if (Instant.now().isAfter(record.expiresAt())) {
            log.warn("Captcha challenge expired for ID: [{}]", captchaId);
            return false;
        }

        boolean valid = record.expectedAnswer().trim().equalsIgnoreCase(userAnswer.trim());
        if (!valid) {
            log.warn("Captcha mismatch for [{}]: expected '{}', got '{}'", captchaId, record.expectedAnswer(), userAnswer);
        }
        return valid;
    }

    private void cleanExpiredEntries() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}
