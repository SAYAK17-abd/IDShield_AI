package com.project.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "generalRequestsPerMinute", 60);
        ReflectionTestUtils.setField(filter, "authRequestsPerMinute", 10);
    }

    @Test
    void doFilter_UnderRateLimit_ShouldProceed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/documents");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilter_ExceedAuthRateLimit_ShouldReturn429TooManyRequests() throws ServletException, IOException {
        String clientIp = "192.168.1.50";

        // First 10 requests should pass
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus(), "Request " + i + " should succeed");
        }

        // 11th request should be blocked with HTTP 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());

        assertEquals(429, blockedResponse.getStatus());
        assertTrue(blockedResponse.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void doFilter_ExceedRefreshTokenRateLimit_ShouldReturn429() throws ServletException, IOException {
        String clientIp = "192.168.1.55";

        // First 10 requests should pass
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        // 11th request should be rate-limited
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/auth/refresh");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());

        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void evictExpiredEntries_ShouldPreventMemoryLeak() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/documents");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(1, filter.getActiveCounterCount());

        // Evict with a 0ms threshold (all current entries considered expired)
        filter.evictExpiredEntries(-1L);
        assertEquals(0, filter.getActiveCounterCount());
    }
}

