package com.project.auth;

import com.project.audit.entity.AuditEventType;
import com.project.audit.service.AuditService;
import com.project.auth.dto.AuthResponse;
import com.project.auth.dto.LoginRequest;
import com.project.auth.dto.LogoutRequest;
import com.project.auth.dto.RefreshTokenRequest;
import com.project.auth.dto.RegisterRequest;
import com.project.auth.entity.RefreshToken;
import com.project.auth.repository.RefreshTokenRepository;
import com.project.auth.service.AuthService;
import com.project.exception.ApiException;
import com.project.exception.UnauthorizedException;
import com.project.security.JwtService;
import com.project.user.dto.UserDto;
import com.project.user.entity.Role; 
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void register_ShouldHashPassword_AndAssignUserRole() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("SecurePass123!")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDto userDto = authService.register(registerReq, request);

        assertNotNull(userDto);
        assertEquals("john@example.com", userDto.getEmail());
        assertEquals(Role.ROLE_USER, userDto.getRole());
        verify(passwordEncoder).encode("SecurePass123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ShouldThrowApiException() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("John Doe")
                .email("duplicate@example.com")
                .password("SecurePass123!")
                .build();

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(registerReq, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshToken_ReuseAttack_ShouldRevokeAllUserTokens() {
        User user = User.builder().id(1L).email("user@example.com").build();
        RefreshToken revokedToken = RefreshToken.builder()
                .id(10L)
                .user(user)
                .tokenHash("somehash")
                .revoked(true)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken("replayedTokenString")
                .build();

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(refreshReq, request));

        // Verify token family revocation occurred
        verify(refreshTokenRepository).revokeAllUserTokens(user);
    }

    @Test
    void login_Success_ShouldGenerateTokens_AndLogAuditSuccess() {
        User user = User.builder()
                .id(42L)
                .email("test@example.com")
                .password("encodedPass")
                .role(Role.ROLE_USER)
                .build();

        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user, 42L, "ROLE_USER")).thenReturn("mockAccessToken");

        AuthResponse authResponse = authService.login(loginRequest, request);

        assertNotNull(authResponse);
        assertEquals("mockAccessToken", authResponse.getAccessToken());
        assertNotNull(authResponse.getRefreshToken());
        verify(auditService).logEvent(
                eq(AuditEventType.LOGIN_SUCCESS),
                eq(42L),
                eq("test@example.com"),
                eq("AUTH"),
                eq("42"),
                eq("User successfully logged in"),
                eq(request)
        );
    }

    @Test
    void login_BadCredentials_ShouldAuditFailure_AndThrowException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("attacker@example.com")
                .password("WrongPassword")
                .build();

        doThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(loginRequest, request));

        verify(auditService).logEvent(
                eq(AuditEventType.LOGIN_FAILURE),
                isNull(),
                eq("attacker@example.com"),
                eq("AUTH"),
                isNull(),
                eq("Invalid password or user not found"),
                eq(request)
        );
    }

    @Test
    void logout_WithAuthenticatedUser_ShouldAssignCorrectUserIdToAuditLog() {
        User user = User.builder().id(99L).email("logoutuser@example.com").build();

        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("logoutuser@example.com", "pass");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("logoutuser@example.com")).thenReturn(Optional.of(user));

        LogoutRequest logoutRequest = LogoutRequest.builder().refreshToken("someRefreshToken").build();

        authService.logout(logoutRequest, request);

        // Verify that audit log received 99L (NOT null!)
        verify(auditService).logEvent(
                eq(AuditEventType.LOGOUT),
                eq(99L),
                eq("logoutuser@example.com"),
                eq("AUTH"),
                eq("99"),
                eq("User logged out"),
                eq(request)
        );

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}

