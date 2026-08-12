package com.community.signal.review.service;

import com.community.signal.review.domain.ReviewUser;
import com.community.signal.review.dto.LoginRequest;
import com.community.signal.review.dto.RegisterRequest;
import com.community.signal.review.dto.TokenResponse;
import com.community.signal.review.repository.ReviewUserRepository;
import com.community.signal.review.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ReviewUserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        var user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("auth.login.failed.user_not_found username={}", request.username());
                    return new UsernameNotFoundException("user.not.found");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("auth.login.failed.bad_credentials username={}", request.username());
            throw new BadCredentialsException("invalid.credentials");
        }

        if (!user.isEnabled()) {
            log.warn("auth.login.failed.disabled_user username={}", request.username());
            throw new DisabledException("user.disabled");
        }

        String token = tokenProvider.generateToken(user.getUsername(), List.of(user.getRole()));
        long expiresIn = tokenProvider.getExpirationMillis();

        log.info("auth.login.success username={}", user.getUsername());
        return new TokenResponse(token, user.getUsername(), user.getRole(), expiresIn);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("username.already.taken");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        ReviewUser newUser = ReviewUser.builder()
                .username(request.username())
                .password(hashedPassword)
                .role(request.role())
                .active(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(newUser);

        String token = tokenProvider.generateToken(newUser.getUsername(), List.of(newUser.getRole()));
        long expiresIn = tokenProvider.getExpirationMillis();

        log.info("auth.register.success username={}", newUser.getUsername());
        return new TokenResponse(token, newUser.getUsername(), newUser.getRole(), expiresIn);
    }
}
