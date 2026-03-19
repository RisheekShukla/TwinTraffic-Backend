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
public class ResponseLogDTO {
    private UUID id;
    private UUID requestId;
    private String source;    // "v1" or "v2"
    private Integer statusCode;
    private Object responseBody;
    private Long latencyMs;
    private String errorMessage;
    private OffsetDateTime createdAt;
}
