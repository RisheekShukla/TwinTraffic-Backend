package com.shadowtool.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comparisons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "match_status", nullable = false, length = 20)
    private String matchStatus; // MATCH, MISMATCH, ERROR, TIMEOUT

    @Column(name = "latency_diff")
    private Long latencyDiff;

    @Column(name = "v1_status_code")
    private Integer v1StatusCode;

    @Column(name = "v2_status_code")
    private Integer v2StatusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_details", columnDefinition = "jsonb")
    private String diffDetails;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
