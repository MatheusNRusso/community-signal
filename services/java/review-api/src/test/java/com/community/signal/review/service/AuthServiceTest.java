package com.community.signal.review.service;

import com.community.signal.review.domain.ReviewUser;
import com.community.signal.review.dto.LoginRequest;
import com.community.signal.review.dto.TokenResponse;
import com.community.signal.review.repository.ReviewUserRepository;
import com.community.signal.review.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ReviewUserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private final ReviewUser activeUser = ReviewUser.builder()
            .id(UUID.randomUUID())
            .username("reviewer-1")
            .password("hashed-password")
            .role("ROLE_REVIEWER")
            .active(true)
            .build();

    @Test
    void login_validCredentials_returnsTokenResponse() {
        // Given
        LoginRequest request = new LoginRequest("reviewer-1", "password123");

        when(userRepository.findByUsername("reviewer-1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(tokenProvider.generateToken(anyString(), any())).thenReturn("jwt-token");
        when(tokenProvider.getExpirationMillis()).thenReturn(86400000L);

        // When
        TokenResponse result = authService.login(request);

        // Then
        assertNotNull(result);
        assertEquals("jwt-token", result.token());
        assertEquals("reviewer-1", result.username());
        assertEquals("ROLE_REVIEWER", result.role());
        assertEquals(86400000L, result.expiresIn());
    }

    @Test
    void login_userNotFound_throwsUsernameNotFoundException() {
        // Given
        LoginRequest request = new LoginRequest("unknown", "password");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        // Given
        LoginRequest request = new LoginRequest("reviewer-1", "wrong");

        when(userRepository.findByUsername("reviewer-1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_disabledUser_throwsDisabledException() {
        // Given
        ReviewUser disabledUser = ReviewUser.builder()
                .id(activeUser.getId())
                .username(activeUser.getUsername())
                .password(activeUser.getPassword())
                .role(activeUser.getRole())
                .active(false)
                .build();

        LoginRequest request = new LoginRequest("reviewer-1", "password123");

        when(userRepository.findByUsername("reviewer-1")).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThrows(DisabledException.class, () -> authService.login(request));
    }
}
