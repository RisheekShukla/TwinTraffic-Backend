package com.shadowtool.controller;

import com.shadowtool.dto.MetricsDTO;
import com.shadowtool.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Platform-level statistics and performance metrics")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Get platform metrics", description = "Returns totals, mismatch rate, avg latency diff, and status breakdown")
    public ResponseEntity<MetricsDTO> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }
}
