package com.shadowtool.controller;

import com.shadowtool.dto.RequestDetailDTO;
import com.shadowtool.dto.RequestLogDTO;
import com.shadowtool.service.RequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Tag(name = "Request Logs", description = "Query logged requests, responses, and comparisons")
public class RequestLogController {

    private final RequestLogService requestLogService;

    @GetMapping
    @Operation(summary = "List all logged requests", description = "Paginated list of all proxied requests, newest first")
    public ResponseEntity<Page<RequestLogDTO>> listRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(requestLogService.listRequests(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request detail", description = "Returns full request with v1/v2 responses and comparison result")
    public ResponseEntity<RequestDetailDTO> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(requestLogService.getDetail(id));
    }
}
