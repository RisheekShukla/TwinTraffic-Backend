package com.shadowtool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Incoming proxy request — will be forwarded to v1 and optionally mirrored to v2")
public class ProxyRequestDTO {

    @NotBlank(message = "endpoint is required")
    @Schema(description = "Target path on both services, e.g. /api/data", example = "/api/data")
    private String endpoint;

    @Schema(description = "HTTP method", example = "POST", defaultValue = "POST")
    private String method = "POST";

    @Schema(description = "Request payload (JSON)")
    private Object payload;

    @Schema(description = "Additional headers to forward")
    private Map<String, String> headers;
}
