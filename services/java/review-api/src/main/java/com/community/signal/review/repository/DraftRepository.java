package com.community.signal.review.repository;

import com.community.signal.review.domain.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID> {

    @Query(value = "SELECT * FROM drafts WHERE status = :status ORDER BY created_at DESC LIMIT :size OFFSET :offset",
           nativeQuery = true)
    List<Draft> findByStatusNative(@Param("status") String status,
                                   @Param("size") int size,
                                   @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM drafts WHERE status = :status",
           nativeQuery = true)
    long countByStatusNative(@Param("status") String status);

    @Modifying
    @Query(value = "UPDATE drafts SET status = :status, updated_at = :updatedAt WHERE id = :id",
           nativeQuery = true)
    void updateStatus(@Param("id") UUID id,
                      @Param("status") String status,
                      @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query(value = "UPDATE drafts SET status = :status, reviewer_id = :reviewerId, reviewer_note = :note, reviewed_at = :reviewedAt, updated_at = :updatedAt WHERE id = :id",
           nativeQuery = true)
    void updateStatusWithReview(@Param("id") UUID id,
                                @Param("status") String status,
                                @Param("reviewerId") String reviewerId,
                                @Param("note") String note,
                                @Param("reviewedAt") Instant reviewedAt,
                                @Param("updatedAt") Instant updatedAt);
}
