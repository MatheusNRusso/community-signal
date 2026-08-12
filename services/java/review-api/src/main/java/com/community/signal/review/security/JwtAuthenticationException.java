package com.community.signal.review.security;

/**
 * Custom exception for JWT authentication failures.
 * Used to signal invalid, expired, or malformed tokens.
 */
public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
