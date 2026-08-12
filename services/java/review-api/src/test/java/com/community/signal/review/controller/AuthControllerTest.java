package com.community.signal.review.controller;

import com.community.signal.review.dto.LoginRequest;
import com.community.signal.review.dto.TokenResponse;
import com.community.signal.review.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_validCredentials_returns200WithToken() {
        // Given
        LoginRequest request = new LoginRequest("reviewer-1", "password123");
        TokenResponse expectedResponse = new TokenResponse("jwt-token", "reviewer-1", "ROLE_REVIEWER", 86400000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        // When
        ResponseEntity<TokenResponse> response = authController.login(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().token());
        assertEquals("reviewer-1", response.getBody().username());
    }

    @Test
    void login_invalidCredentials_returns401() {
        // Given
        LoginRequest request = new LoginRequest("reviewer-1", "wrong");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("invalid.credentials"));

        // When
        ResponseEntity<TokenResponse> response = authController.login(request);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void login_disabledUser_returns403() {
        // Given
        LoginRequest request = new LoginRequest("disabled-user", "password");

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new DisabledException("user.disabled"));

        // When
        ResponseEntity<TokenResponse> response = authController.login(request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getBody());
    }
}
