package com.shadowtool.v2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api")
public class V2DataController {

    @Value("${v2.chaos.enabled:true}")
    private boolean chaosEnabled;

    @Value("${v2.chaos.delay-min-ms:2000}")
    private int delayMinMs;

    @Value("${v2.chaos.delay-max-ms:6000}")
    private int delayMaxMs;

    private static final Random RANDOM = new Random();

    /**
     * 40% correct, 30% error, 30% delayed
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(@RequestHeader Map<String, String> headers) {
        return handleChaos("GET", null, headers);
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> postData(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        return handleChaos("POST", payload, headers);
    }

    @RequestMapping("/**")
    public ResponseEntity<Map<String, Object>> handleAny(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        return handleChaos("ANY", payload, headers);
    }

    private ResponseEntity<Map<String, Object>> handleChaos(String method, Object payload,
                                                             Map<String, String> headers) {
        boolean isShadow = "true".equalsIgnoreCase(headers.get("x-shadow-request"));
        log.info("[v2] Received {} request (shadow={})", method, isShadow);

        if (!chaosEnabled) {
            return ResponseEntity.ok(buildCorrectResponse(method, payload));
        }

        double roll = RANDOM.nextDouble(); // 0.0 - 1.0

        if (roll < 0.40) {
            // 40% — correct response
            log.info("[v2] [CORRECT] Returning correct response");
            return ResponseEntity.ok(buildCorrectResponse(method, payload));

        } else if (roll < 0.70) {
            // 30% — error response
            int errorCode = pickErrorCode();
            log.warn("[v2] [ERROR] Simulating error {}", errorCode);
            return ResponseEntity.status(errorCode).body(buildErrorResponse(errorCode));

        } else {
            // 30% — delayed response (latency simulation)
            int delay = delayMinMs + RANDOM.nextInt(delayMaxMs - delayMinMs + 1);
            log.warn("[v2] [DELAY] Sleeping {}ms to simulate latency", delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // May return correct or slightly different body after delay
            Map<String, Object> response = buildCorrectResponse(method, payload);
            response.put("simulatedLatencyMs", delay);
            return ResponseEntity.ok(response);
        }
    }

    private Map<String, Object> buildCorrectResponse(String method, Object payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "v2");
        response.put("status", "ok");
        response.put("message", "Shadow service response");
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

    private Map<String, Object> buildErrorResponse(int code) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "v2");
        response.put("status", "error");
        response.put("errorCode", code);
        response.put("message", code == 503 ? "Service unavailable (simulated)" : "Internal server error (simulated)");
        response.put("timestamp", Instant.now().toEpochMilli());
        return response;
    }

    private int pickErrorCode() {
        return RANDOM.nextBoolean() ? 500 : 503;
    }
}
