package com.community.signal.review.service;

import com.community.signal.review.domain.Draft;
import com.community.signal.review.domain.DraftStatus;
import com.community.signal.review.exception.DraftNotFoundException;
import com.community.signal.review.exception.InvalidStateTransitionException;
import com.community.signal.review.kafka.DraftEventPublisher;
import com.community.signal.review.repository.DraftRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core review workflow service.
 * Handles state transitions: PENDING -> IN_REVIEW -> APPROVED | REJECTED | REVISED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final Map<DraftStatus, java.util.Set<DraftStatus>> VALID_TRANSITIONS = Map.of(
            DraftStatus.PENDING,   java.util.Set.of(DraftStatus.IN_REVIEW, DraftStatus.REJECTED),
            DraftStatus.IN_REVIEW, java.util.Set.of(DraftStatus.APPROVED, DraftStatus.REJECTED, DraftStatus.REVISED),
            DraftStatus.REVISED,   java.util.Set.of(DraftStatus.APPROVED, DraftStatus.REJECTED)
    );

    private final DraftRepository    draftRepository;
    private final EntityManager        entityManager;
    private final DraftEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<Draft> listPendingDrafts(Pageable pageable) {
        int size   = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        java.util.List<Draft> content = draftRepository.findByStatusNative("PENDING", size, offset);
        long total = draftRepository.countByStatusNative("PENDING");
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<Draft> listDraftsByStatus(DraftStatus status, Pageable pageable) {
        int size   = pageable.getPageSize();
        int offset = (int) pageable.getOffset();
        String s   = status.name();
        java.util.List<Draft> content = draftRepository.findByStatusNative(s, size, offset);
        long total = draftRepository.countByStatusNative(s);
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public Draft getDraft(UUID id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> new DraftNotFoundException(id));
    }

    @Transactional
    public Draft startReview(UUID id, String reviewerId) {
        Draft draft = getDraft(id);
        validateTransition(draft.getStatus(), DraftStatus.IN_REVIEW);
        entityManager.detach(draft);
        draftRepository.updateStatus(id, "IN_REVIEW", Instant.now());
        draft.setStatus(DraftStatus.IN_REVIEW);
        draft.setReviewerId(reviewerId);
        log.info("draft.review.started draftId={} reviewerId={}", id, reviewerId);
        return draft;
    }

    @Transactional
    public Draft approveDraft(UUID id, String reviewerId, String note) {
        Draft draft = getDraft(id);
        validateTransition(draft.getStatus(), DraftStatus.APPROVED);
        entityManager.detach(draft);
        Instant now = Instant.now();
        draftRepository.updateStatusWithReview(id, "APPROVED", reviewerId, note, now, now);
        draft.setStatus(DraftStatus.APPROVED);
        draft.setReviewerId(reviewerId);
        draft.setReviewerNote(note);
        draft.setReviewedAt(now);
        eventPublisher.publishApproved(draft);
        log.info("draft.approved draftId={} reviewerId={}", id, reviewerId);
        return draft;
    }

    @Transactional
    public Draft rejectDraft(UUID id, String reviewerId, String note) {
        Draft draft = getDraft(id);
        validateTransition(draft.getStatus(), DraftStatus.REJECTED);
        entityManager.detach(draft);
        Instant now = Instant.now();
        draftRepository.updateStatusWithReview(id, "REJECTED", reviewerId, note, now, now);
        draft.setStatus(DraftStatus.REJECTED);
        draft.setReviewerId(reviewerId);
        draft.setReviewerNote(note);
        draft.setReviewedAt(now);
        log.info("draft.rejected draftId={} reviewerId={}", id, reviewerId);
        return draft;
    }

    @Transactional
    public Draft reviseDraft(UUID id, String reviewerId, String revisedContent, String note) {
        Draft draft = getDraft(id);
        validateTransition(draft.getStatus(), DraftStatus.REVISED);

        draft.setStatus(DraftStatus.REVISED);
        draft.setContent(revisedContent);
        draft.setReviewerId(reviewerId);
        draft.setReviewerNote(note);
        draft.setReviewedAt(Instant.now());

        Draft saved = draftRepository.save(draft);
        log.info("draft.revised draftId={} reviewerId={}", id, reviewerId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        return Map.of(
                "pending",   draftRepository.countByStatusNative("PENDING"),
                "in_review", draftRepository.countByStatusNative("IN_REVIEW"),
                "approved",  draftRepository.countByStatusNative("APPROVED"),
                "rejected",  draftRepository.countByStatusNative("REJECTED")
        );
    }

    private void validateTransition(DraftStatus from, DraftStatus to) {
        java.util.Set<DraftStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, java.util.Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
