package com.community.signal.review.repository;

import com.community.signal.review.domain.Draft;
import com.community.signal.review.domain.DraftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID> {

    Page<Draft> findByStatusOrderByCreatedAtDesc(DraftStatus status, Pageable pageable);

    long countByStatus(DraftStatus status);

    @Modifying
    @Query("UPDATE Draft d SET d.status = :status, d.updatedAt = :updatedAt WHERE d.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") DraftStatus status,
                      @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("UPDATE Draft d SET d.status = :status, d.reviewerId = :reviewerId, " +
           "d.reviewerNote = :note, d.reviewedAt = :reviewedAt, d.updatedAt = :updatedAt WHERE d.id = :id")
    void updateStatusWithReview(@Param("id") UUID id,
                                @Param("status") DraftStatus status,
                                @Param("reviewerId") String reviewerId,
                                @Param("note") String note,
                                @Param("reviewedAt") Instant reviewedAt,
                                @Param("updatedAt") Instant updatedAt);
}
