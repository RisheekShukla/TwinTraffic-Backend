package com.shadowtool.v1.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class V1DataController {

    /**
     * GET /api/data — always returns a correct, stable response
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(@RequestHeader Map<String, String> headers) {
        logShadowHeader(headers);
        return ResponseEntity.ok(buildResponse("GET", null));
    }

    /**
     * POST /api/data — echoes payload with stable response
     */
    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> postData(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        logShadowHeader(headers);
        return ResponseEntity.ok(buildResponse("POST", payload));
    }

    /**
     * Generic catch-all for other paths
     */
    @RequestMapping("/**")
    public ResponseEntity<Map<String, Object>> handleAny(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        logShadowHeader(headers);
        return ResponseEntity.ok(buildResponse("ANY", payload));
    }

    private Map<String, Object> buildResponse(String method, Object payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "v1");
        response.put("status", "ok");
        response.put("message", "Primary service response");
        response.put("method", method);
        response.put("timestamp", Instant.now().toEpochMilli());
        response.put("data", Map.of(
                "id", 1001,
                "name", "ShadowTool Item",
                "value", 42.0,
                "active", true
        ));
        if (payload != null) {
            response.put("echo", payload);
        }
        return response;
    }

    private void logShadowHeader(Map<String, String> headers) {
        if ("true".equalsIgnoreCase(headers.get("x-shadow-request"))) {
            log.info("[v1] Received shadow request (X-Shadow-Request: true)");
        }
    }
}
