package com.project.auth;

import com.project.audit.service.AuditService;
import com.project.auth.dto.AuthResponse;
import com.project.auth.dto.LoginRequest;
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
}

