package com.community.signal.review.service;

import com.community.signal.review.domain.ReviewUser;
import com.community.signal.review.repository.ReviewUserRepository;
import com.community.signal.review.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {

    private final ReviewUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth.allowed-github-users:}")
    private String allowedGithubUsers;

    /**
     * Handles successful GitHub OAuth login.
     * Creates or updates user and generates internal JWT.
     */
    @Transactional
    public String handleOAuthSuccess(Authentication authentication) {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        Long githubId = ((Number) oauthUser.getAttribute("id")).longValue();
        String login = oauthUser.getAttribute("login");
        String avatarUrl = oauthUser.getAttribute("avatar_url");

        if (login == null || login.isBlank()) {
            log.error("github.oauth.failed reason=missing_login");
            throw new IllegalStateException("GitHub login missing username");
        }

        // ── Check allowlist ─────────────────────────────────────────────────
        if (!isUserAllowed(login)) {
            log.warn("github.oauth.denied reason=user_not_in_allowlist github_username={}", login);
            throw new SecurityException("User not authorized: " + login);
        }

        // ── Upsert user ─────────────────────────────────────────────────────
        ReviewUser user = userRepository.findByGithubId(githubId)
                .orElseGet(() -> userRepository.findByGithubUsername(login)
                        .orElseGet(() -> {
                            log.info("github.oauth.creating_new_user github_username={}", login);
                            return ReviewUser.builder()
                                    .id(UUID.randomUUID())
                                    .username(login)
                                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                    .role("ROLE_REVIEWER")
                                    .active(true)
                                    .createdAt(Instant.now())
                                    .build();
                        }));

        // ── Update GitHub data ──────────────────────────────────────────────
        user.setGithubId(githubId);
        user.setGithubUsername(login);
        user.setAvatarUrl(avatarUrl);
        user.markAsGithubUser();

        userRepository.save(user);

        log.info("github.oauth.success user_id={} github_username={}", user.getId(), login);

        // ── Generate JWT ────────────────────────────────────────────────────
        return jwtTokenProvider.generateToken(user.getUsername(), List.of(user.getRole()));
    }

    /**
     * Checks if GitHub username is in the allowlist.
     * Empty allowlist = allow all (for development).
     */
    private boolean isUserAllowed(String githubUsername) {
        if (allowedGithubUsers == null || allowedGithubUsers.isBlank()) {
            log.debug("github.oauth.allowlist_empty allowing_all_users");
            return true;
        }

        List<String> allowed = Arrays.stream(allowedGithubUsers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (allowed.isEmpty()) {
            return true;
        }

        return allowed.stream()
                .anyMatch(allowedUser -> allowedUser.equalsIgnoreCase(githubUsername));
    }
}
