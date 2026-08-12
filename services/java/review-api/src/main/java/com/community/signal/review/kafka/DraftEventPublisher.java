package com.community.signal.review.kafka;

import com.community.signal.review.domain.Draft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j
public class DraftEventPublisher {
    @Value("${kafka.topics.approved-drafts:approved-drafts}") private String approvedDraftsTopic;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishApproved(Draft draft) {
        Map<String, Object> payload = Map.of(
            "id", draft.getId().toString(),
            "cluster_id", draft.getClusterId(),
            "channel", draft.getChannel().name(),
            "content", draft.getContent(),
            "reviewer_id", draft.getReviewerId(),
            "reviewed_at", draft.getReviewedAt().toString(),
            "schema_version", "1.0.0"
        );
        kafkaTemplate.send(approvedDraftsTopic, draft.getClusterId(), payload)
            .whenComplete((r, ex) -> {
                if (ex != null) log.error("draft.publish.failed draftId={}", draft.getId());
                else log.info("draft.published draftId={}", draft.getId());
            });
    }
}
