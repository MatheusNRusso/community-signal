package com.community.signal.review.dto;

public record TokenResponse(
    String token,
    String username,
    String role,
    long expiresIn
) {}
