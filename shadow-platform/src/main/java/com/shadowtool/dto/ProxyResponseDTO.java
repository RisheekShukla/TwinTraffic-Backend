package com.shadowtool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned to the client — reflects v1 outcome")
public class ProxyResponseDTO {

    @Schema(description = "Logged request ID for future retrieval/replay")
    private UUID requestId;

    @Schema(description = "HTTP status code returned by v1")
    private int statusCode;

    @Schema(description = "Response body from v1")
    private Object body;

    @Schema(description = "Time taken by v1 in milliseconds")
    private long latencyMs;

    @Schema(description = "Whether shadow mirroring was triggered")
    private boolean shadowTriggered;
}
