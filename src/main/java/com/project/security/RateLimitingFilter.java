package com.project.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-Memory Token Bucket / Window Rate Limiting Filter.
 *
 * Prototype Implementation:
 * Uses a thread-safe ConcurrentHashMap per client IP to safeguard expensive/sensitive endpoints:
 * - /api/auth/login, /api/auth/register (brute-force defense)
 * - /api/documents/upload (DoS & storage abuse defense)
 *
 * Production Note:
 * For a distributed, multi-instance deployment across Kubernetes/load balancers,
 * replace this in-memory map with a distributed Redis-based limiter (e.g. Bucket4j + Lettuce).
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${application.security.rate-limit.requests-per-minute:60}")
    private int generalRequestsPerMinute;

    @Value("${application.security.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStartTime = System.currentTimeMillis();

        public int incrementAndGet(long windowMs) {
            long now = System.currentTimeMillis();
            if (now - windowStartTime > windowMs) {
                synchronized (this) {
                    if (now - windowStartTime > windowMs) {
                        count.set(0);
                        windowStartTime = now;
                    }
                }
            }
            return count.incrementAndGet();
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        int maxAllowed = generalRequestsPerMinute;
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            maxAllowed = authRequestsPerMinute;
        }

        String key = clientIp + ":" + (path.startsWith("/api/auth/") ? "auth" : "gen");
        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());

        int currentRequests = counter.incrementAndGet(60000L); // 1 minute window

        if (currentRequests > maxAllowed) {
            log.warn("Rate limit exceeded for IP: {} on path: {} (Count: {})", clientIp, path, currentRequests);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("RATE_LIMIT_EXCEEDED")
                    .message("Too many requests. Please slow down and try again after one minute.")
                    .path(path)
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

