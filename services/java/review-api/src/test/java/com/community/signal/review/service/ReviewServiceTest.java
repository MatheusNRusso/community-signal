package com.community.signal.review.service;

import com.community.signal.review.domain.*;
import com.community.signal.review.exception.*;
import com.community.signal.review.kafka.DraftEventPublisher;
import com.community.signal.review.repository.DraftRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock DraftRepository    draftRepository;
    @Mock DraftEventPublisher eventPublisher;
    @Mock EntityManager       entityManager;
    @InjectMocks ReviewService reviewService;

    private Draft pendingDraft;

    @BeforeEach void setUp() {
        pendingDraft = Draft.builder()
            .id(UUID.randomUUID()).windowId("w1").clusterId("c1")
            .channel(Channel.LINKEDIN)
            .content("Draft content about Python ecosystem.")
            .guardrailScore(0.95).guardrailPassed(true)
            .llmModel("claude-sonnet-4-20250514")
            .status(DraftStatus.PENDING).build();
    }

    @Test void getDraft_found() {
        when(draftRepository.findById(pendingDraft.getId())).thenReturn(Optional.of(pendingDraft));
        assertThat(reviewService.getDraft(pendingDraft.getId()).getId()).isEqualTo(pendingDraft.getId());
    }

    @Test void getDraft_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(draftRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewService.getDraft(id)).isInstanceOf(DraftNotFoundException.class);
    }

    @Test void startReview_updatesStatus() {
        when(draftRepository.findById(pendingDraft.getId())).thenReturn(Optional.of(pendingDraft));
        doNothing().when(entityManager).detach(any());
        doNothing().when(draftRepository).updateStatus(any(), any(), any());
        Draft result = reviewService.startReview(pendingDraft.getId(), "r1");
        assertThat(result.getStatus()).isEqualTo(DraftStatus.IN_REVIEW);
    }

    @Test void approveDraft_publishesEvent() {
        pendingDraft.setStatus(DraftStatus.IN_REVIEW);
        when(draftRepository.findById(pendingDraft.getId())).thenReturn(Optional.of(pendingDraft));
        doNothing().when(entityManager).detach(any());
        doNothing().when(draftRepository).updateStatusWithReview(any(), any(), any(), any(), any(), any());
        Draft result = reviewService.approveDraft(pendingDraft.getId(), "r1", "Good");
        assertThat(result.getStatus()).isEqualTo(DraftStatus.APPROVED);
        verify(eventPublisher).publishApproved(result);
    }

    @Test void rejectDraft_noPublish() {
        pendingDraft.setStatus(DraftStatus.IN_REVIEW);
        when(draftRepository.findById(pendingDraft.getId())).thenReturn(Optional.of(pendingDraft));
        doNothing().when(entityManager).detach(any());
        doNothing().when(draftRepository).updateStatusWithReview(any(), any(), any(), any(), any(), any());
        Draft result = reviewService.rejectDraft(pendingDraft.getId(), "r1", "Off-brand");
        assertThat(result.getStatus()).isEqualTo(DraftStatus.REJECTED);
        verifyNoInteractions(eventPublisher);
    }

    @Test void invalidTransition_throws() {
        pendingDraft.setStatus(DraftStatus.REJECTED);
        when(draftRepository.findById(pendingDraft.getId())).thenReturn(Optional.of(pendingDraft));
        assertThatThrownBy(() -> reviewService.approveDraft(pendingDraft.getId(), "r1", "note"))
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test void getStats_returnsAll() {
        when(draftRepository.countByStatusNative("PENDING")).thenReturn(5L);
        when(draftRepository.countByStatusNative("IN_REVIEW")).thenReturn(2L);
        when(draftRepository.countByStatusNative("APPROVED")).thenReturn(10L);
        when(draftRepository.countByStatusNative("REJECTED")).thenReturn(3L);
        var stats = reviewService.getStats();
        assertThat(stats).containsEntry("pending", 5L).containsEntry("approved", 10L);
    }
}
