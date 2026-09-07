package com.project.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String STRONG_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", STRONG_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "activeProfile", "dev");
    }

    @Test
    void validateConfiguration_MissingSecretKey_ShouldThrowIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "");
        assertThrows(IllegalStateException.class, () -> jwtService.validateConfiguration());
    }

    @Test
    void validateConfiguration_InsecureKeyInProduction_ShouldThrowIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "activeProfile", "prod");

        assertThrows(IllegalStateException.class, () -> jwtService.validateConfiguration());
    }

    @Test
    void validateConfiguration_StrongKeyInProduction_ShouldSucceed() {
        ReflectionTestUtils.setField(jwtService, "secretKey", STRONG_KEY);
        ReflectionTestUtils.setField(jwtService, "activeProfile", "prod");

        assertDoesNotThrow(() -> jwtService.validateConfiguration());
    }

    @Test
    void generateAndValidateAccessToken_ShouldSucceed() {
        UserDetails user = new User("officer@idshield.gov.in", "pass", Collections.emptyList());

        String token = jwtService.generateAccessToken(user, 100L, "ROLE_INVESTIGATOR");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, user));
        assertEquals("officer@idshield.gov.in", jwtService.extractUsername(token));
    }
}

