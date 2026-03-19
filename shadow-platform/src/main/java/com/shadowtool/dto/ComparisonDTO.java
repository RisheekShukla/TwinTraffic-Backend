package com.shadowtool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonDTO {
    private UUID id;
    private UUID requestId;
    private String matchStatus;  // MATCH, MISMATCH, ERROR, TIMEOUT
    private Long latencyDiff;
    private Integer v1StatusCode;
    private Integer v2StatusCode;
    private Object diffDetails;
    private OffsetDateTime createdAt;
}
