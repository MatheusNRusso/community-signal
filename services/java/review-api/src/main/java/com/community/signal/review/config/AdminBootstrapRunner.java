package com.community.signal.review.config;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final ReviewUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:#{null}}")
    private String adminUsername;

    @Value("${admin.password:#{null}}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        // 1. Validate environment variables
        if (isBlank(adminUsername) || isBlank(adminPassword)) {
            log.warn("admin.bootstrap.skipped reason=env_vars_missing_or_empty vars=ADMIN_USERNAME,ADMIN_PASSWORD");
            return;
        }

        // 2. Check idempotency (do not create if an admin already exists)
        if (userRepository.existsByRole("ROLE_ADMIN")) {
            log.info("admin.bootstrap.skipped reason=admin_already_exists");
            return;
        }

        // 3. Bootstrap initial admin
        ReviewUser admin = ReviewUser.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .role("ROLE_ADMIN")
                .active(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(admin);

        log.info("admin.bootstrap.success username={}", adminUsername);
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
