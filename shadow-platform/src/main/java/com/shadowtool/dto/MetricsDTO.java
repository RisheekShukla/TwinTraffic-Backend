package com.shadowtool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Platform-wide metrics summary")
public class MetricsDTO {

    @Schema(description = "Total number of proxied requests")
    private long totalRequests;

    @Schema(description = "Total number of mirrored shadow requests")
    private long totalShadowRequests;

    @Schema(description = "Total mismatches detected")
    private long totalMismatches;

    @Schema(description = "Total v2 timeouts or errors")
    private long totalErrors;

    @Schema(description = "Average absolute latency difference in ms (v1 vs v2)")
    private Double avgLatencyDiffMs;

    @Schema(description = "Match status breakdown: { MATCH: N, MISMATCH: N, ERROR: N, TIMEOUT: N }")
    private Map<String, Long> matchStatusBreakdown;

    @Schema(description = "Mismatch rate as a percentage")
    private Double mismatchRatePercent;
}
