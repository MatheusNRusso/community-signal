package com.community.signal.review.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "drafts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Draft {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "window_id", nullable = false) private String windowId;
    @Column(name = "cluster_id", nullable = false) private String clusterId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Channel channel;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "guardrail_score") private Double guardrailScore;
    @Column(name = "guardrail_passed") private Boolean guardrailPassed;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "guardrail_reasons", columnDefinition = "jsonb") private List<String> guardrailReasons;
    @Column(name = "llm_model") private String llmModel;
    @Column(name = "prompt_version") private String promptVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) @Builder.Default private DraftStatus status = DraftStatus.PENDING;
    @Column(name = "reviewer_id") private String reviewerId;
    @Column(name = "reviewer_note", columnDefinition = "TEXT") private String reviewerNote;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "generated_at") private Instant generatedAt;
    @Column(name = "created_at", updatable = false) @Builder.Default private Instant createdAt = Instant.now();
    @Column(name = "updated_at") @Builder.Default private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }
}
