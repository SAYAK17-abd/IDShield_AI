package com.project.security;

import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for issuing and verifying HMAC-SHA256 signed JSON Web Tokens (JJWT 0.12+).
 * Access tokens are short-lived and cryptographically signed.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key:}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration:900000}")
    private long jwtExpirationMs; // 15 minutes default

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET configuration is missing! A 256-bit secret key must be provided via the JWT_SECRET environment variable."
            );
        }

        boolean isDevOrTest = activeProfile != null && (
                activeProfile.contains("dev") || activeProfile.contains("test")
        );

        if (!isDevOrTest) {
            if (secretKey.equals("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
                    || secretKey.toLowerCase().contains("secret")
                    || secretKey.trim().length() < 32) {
                throw new IllegalStateException(
                        "Insecure JWT_SECRET detected in production environment! Please set a strong, unique 256-bit secret key via JWT_SECRET."
                );
            }
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Ensure minimum 256-bit key length for HMAC-SHA256
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm missing", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails, Long userId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);
        return buildToken(extraClaims, userDetails.getUsername(), jwtExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

