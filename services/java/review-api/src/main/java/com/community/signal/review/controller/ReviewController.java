package com.community.signal.review.controller;

import com.community.signal.review.domain.Draft;
import com.community.signal.review.domain.DraftStatus;
import com.community.signal.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Review API — HITL endpoints for draft management.
 *
 * GET  /api/drafts              — list drafts (filterable by status)
 * GET  /api/drafts/{id}         — get single draft
 * POST /api/drafts/{id}/review  — start review
 * POST /api/drafts/{id}/approve — approve draft
 * POST /api/drafts/{id}/reject  — reject draft
 * POST /api/drafts/{id}/revise  — revise content
 * GET  /api/drafts/stats        — queue stats
 */
@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<Draft>> listDrafts(
            @RequestParam(defaultValue = "PENDING") DraftStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.listDraftsByStatus(status, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(reviewService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Draft> getDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getDraft(id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Draft> startReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewerRequest req
    ) {
        return ResponseEntity.ok(reviewService.startReview(id, req.reviewerId()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Draft> approveDraft(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalRequest req
    ) {
        return ResponseEntity.ok(reviewService.approveDraft(id, req.reviewerId(), req.note()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Draft> rejectDraft(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequest req
    ) {
        return ResponseEntity.ok(reviewService.rejectDraft(id, req.reviewerId(), req.note()));
    }

    @PostMapping("/{id}/revise")
    public ResponseEntity<Draft> reviseDraft(
            @PathVariable UUID id,
            @Valid @RequestBody RevisionRequest req
    ) {
        return ResponseEntity.ok(
                reviewService.reviseDraft(id, req.reviewerId(), req.content(), req.note())
        );
    }



    // ── Request records ───────────────────────────────────────────────────────

    record ReviewerRequest(
            @NotBlank String reviewerId
    ) {}

    record ApprovalRequest(
            @NotBlank String reviewerId,
            String note
    ) {}

    record RejectionRequest(
            @NotBlank String reviewerId,
            @NotBlank @Size(min = 5) String note
    ) {}

    record RevisionRequest(
            @NotBlank String reviewerId,
            @NotBlank @Size(min = 10) String content,
            String note
    ) {}
}
