package com.community.signal.review.kafka;

import com.community.signal.review.domain.Draft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DraftEventPublisher {

    @Value("${kafka.topics.approved-drafts:approved-drafts}")
    private String approvedDraftsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish approved draft event to Kafka.
     * Failures are logged but never propagated — Kafka is best-effort,
     * the primary transaction (DB write) must not be blocked by broker issues.
     */
    public void publishApproved(Draft draft) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", draft.getId().toString(),
                    "cluster_id", draft.getClusterId(),
                    "channel", draft.getChannel().name(),
                    "content", draft.getContent(),
                    "reviewer_id", String.valueOf(draft.getReviewerId()),
                    "reviewed_at", String.valueOf(draft.getReviewedAt()),
                    "schema_version", "1.0.0"
            );
            kafkaTemplate.send(approvedDraftsTopic, draft.getClusterId(), payload)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.warn("draft.publish.failed draftId={} reason={}", draft.getId(), ex.getMessage());
                        } else {
                            log.info("draft.published draftId={}", draft.getId());
                        }
                    });
        } catch (Exception e) {
            // Swallow: Kafka is best-effort, DB state is authoritative
            log.warn("draft.publish.skipped draftId={} reason={}", draft.getId(), e.getMessage());
        }
    }
}
