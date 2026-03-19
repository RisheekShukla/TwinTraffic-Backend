package com.shadowtool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.entity.ComparisonEntity;
import com.shadowtool.entity.ResponseEntity;
import com.shadowtool.repository.ComparisonRepository;
import com.shadowtool.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonEngine {

    private final ResponseRepository responseRepository;
    private final ComparisonRepository comparisonRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void compare(UUID requestId) {
        List<ResponseEntity> responses = responseRepository.findByRequestId(requestId);

        Optional<ResponseEntity> v1Opt = responses.stream()
                .filter(r -> "v1".equals(r.getSource())).findFirst();
        Optional<ResponseEntity> v2Opt = responses.stream()
                .filter(r -> "v2".equals(r.getSource())).findFirst();

        if (v1Opt.isEmpty() || v2Opt.isEmpty()) {
            log.warn("[comparison] Missing v1 or v2 response for requestId={}", requestId);
            return;
        }

        ResponseEntity v1 = v1Opt.get();
        ResponseEntity v2 = v2Opt.get();

        String matchStatus;
        Map<String, Object> diffDetails = new LinkedHashMap<>();
        long latencyDiff = Math.abs(
                (v1.getLatencyMs() != null ? v1.getLatencyMs() : 0L) -
                (v2.getLatencyMs() != null ? v2.getLatencyMs() : 0L)
        );

        // Case 1: v2 timed out or had a non-HTTP error
        if (v2.getErrorMessage() != null && v2.getErrorMessage().startsWith("TIMEOUT")) {
            matchStatus = "TIMEOUT";
            diffDetails.put("reason", "v2 timed out");
        }
        // Case 2: status code mismatch
        else if (!Objects.equals(v1.getStatusCode(), v2.getStatusCode())) {
            matchStatus = "MISMATCH";
            diffDetails.put("statusCodeMismatch", Map.of(
                    "v1", v1.getStatusCode(),
                    "v2", v2.getStatusCode()
            ));
            // Also diff bodies
            Map<String, Object> bodyDiff = diffJsonBodies(v1.getResponseBody(), v2.getResponseBody());
            if (!bodyDiff.isEmpty()) {
                diffDetails.put("bodyDiff", bodyDiff);
            }
        }
        // Case 3: v2 error
        else if (v2.getErrorMessage() != null) {
            matchStatus = "ERROR";
            diffDetails.put("v2Error", v2.getErrorMessage());
        }
        // Case 4: compare bodies
        else {
            Map<String, Object> bodyDiff = diffJsonBodies(v1.getResponseBody(), v2.getResponseBody());
            if (bodyDiff.isEmpty()) {
                matchStatus = "MATCH";
            } else {
                matchStatus = "MISMATCH";
                diffDetails.put("bodyDiff", bodyDiff);
            }
        }

        String diffJson = toJson(diffDetails);

        ComparisonEntity comparison = ComparisonEntity.builder()
                .requestId(requestId)
                .matchStatus(matchStatus)
                .latencyDiff(latencyDiff)
                .v1StatusCode(v1.getStatusCode())
                .v2StatusCode(v2.getStatusCode())
                .diffDetails(diffJson)
                .build();

        comparisonRepository.save(comparison);
        log.info("[comparison] requestId={} => {} latencyDiff={}ms", requestId, matchStatus, latencyDiff);
    }

    /**
     * Recursively diff two JSON strings. Returns a map of differing fields.
     */
    private Map<String, Object> diffJsonBodies(String body1, String body2) {
        Map<String, Object> diffs = new LinkedHashMap<>();
        if (body1 == null && body2 == null) return diffs;
        if (body1 == null || body2 == null) {
            diffs.put("nullDiff", Map.of("v1", body1 == null ? "null" : "present",
                                          "v2", body2 == null ? "null" : "present"));
            return diffs;
        }

        try {
            JsonNode tree1 = objectMapper.readTree(body1);
            JsonNode tree2 = objectMapper.readTree(body2);
            diffNodes("root", tree1, tree2, diffs);
        } catch (Exception e) {
            // Not valid JSON — do string comparison
            if (!body1.equals(body2)) {
                diffs.put("rawMismatch", Map.of("v1", body1, "v2", body2));
            }
        }
        return diffs;
    }

    private void diffNodes(String path, JsonNode n1, JsonNode n2, Map<String, Object> diffs) {
        if (n1 == null && n2 == null) return;
        if (n1 == null || n2 == null || !n1.equals(n2)) {
            if (n1 != null && n2 != null && n1.isObject() && n2.isObject()) {
                Set<String> keys = new HashSet<>();
                n1.fieldNames().forEachRemaining(keys::add);
                n2.fieldNames().forEachRemaining(keys::add);
                for (String key : keys) {
                    diffNodes(path + "." + key, n1.get(key), n2.get(key), diffs);
                }
            } else {
                diffs.put(path, Map.of(
                        "v1", n1 != null ? n1.toString() : "missing",
                        "v2", n2 != null ? n2.toString() : "missing"
                ));
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
