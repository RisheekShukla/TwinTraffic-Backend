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
public class RequestLogDTO {
    private UUID id;
    private String endpoint;
    private String method;
    private Object payload;
    private Object headers;
    private OffsetDateTime createdAt;
}
