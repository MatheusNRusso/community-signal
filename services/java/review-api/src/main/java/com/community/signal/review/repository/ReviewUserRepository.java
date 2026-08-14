package com.community.signal.review.repository;

import com.community.signal.review.domain.ReviewUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewUserRepository extends JpaRepository<ReviewUser, UUID> {

    Optional<ReviewUser> findByUsername(String username);

    Optional<ReviewUser> findByGithubId(Long githubId);

    Optional<ReviewUser> findByGithubUsername(String githubUsername);

    boolean existsByUsername(String username);
}
