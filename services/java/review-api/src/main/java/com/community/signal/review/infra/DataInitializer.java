package com.community.signal.review.infra;

import com.community.signal.review.domain.ReviewUser;
import com.community.signal.review.repository.ReviewUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final ReviewUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("admin.user.not.created reason=missing_email_or_password");
            return;
        }

        String email = adminEmail.toLowerCase().trim();

        if (userRepository.existsByUsername(email)) {
            log.info("admin.user.already.exists email={}", email);
            return;
        }

        ReviewUser admin = ReviewUser.builder()
                .id(UUID.randomUUID())
                .username(email)
                .password(passwordEncoder.encode(adminPassword))
                .role("ROLE_ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(admin);

        log.info("admin.user.created email={}", email);
    }
}
