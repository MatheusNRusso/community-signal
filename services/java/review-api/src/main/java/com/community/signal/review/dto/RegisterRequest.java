package com.community.signal.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 100) String username,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank String role
) {
    /**
     * Compact constructor to apply default role if not provided in the payload.
     */
    public RegisterRequest {
        if (role == null || role.isBlank()) {
            role = "ROLE_REVIEWER";
        }
    }
}
