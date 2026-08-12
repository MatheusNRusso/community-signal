package com.community.signal.review.repository;

import com.community.signal.review.domain.ReviewUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewUserRepository extends JpaRepository<ReviewUser, UUID> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an Optional containing the ReviewUser if found, or empty if not
     */
    Optional<ReviewUser> findByUsername(String username);

    /**
     * Checks if a user with the given username already exists.
     *
     * @param username the username to check
     * @return true if a user with the username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if any user with the specified role exists.
     */
    boolean existsByRole(String role);
}
