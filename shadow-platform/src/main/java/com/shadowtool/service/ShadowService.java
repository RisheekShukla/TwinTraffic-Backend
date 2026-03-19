package com.shadowtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.config.ShadowConfig;
import com.shadowtool.dto.ProxyRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ShadowService {

    private final ShadowConfig shadowConfig;
    private final LoggingService loggingService;
    private final ComparisonEngine comparisonEngine;
    private final ObjectMapper objectMapper;
    private final WebClient v2WebClient;

    @Autowired
    public ShadowService(ShadowConfig shadowConfig,
                         LoggingService loggingService,
                         ComparisonEngine comparisonEngine,
                         ObjectMapper objectMapper,
                         @Qualifier("v2WebClient") WebClient v2WebClient) {
        this.shadowConfig = shadowConfig;
        this.loggingService = loggingService;
        this.comparisonEngine = comparisonEngine;
        this.objectMapper = objectMapper;
        this.v2WebClient = v2WebClient;
    }

    /**
     * Asynchronously mirrors the request to v2, stores the response,
     * then triggers comparison. v2 failures NEVER propagate to the caller.
     */
    @Async("shadowTaskExecutor")
    public void mirrorAsync(UUID requestId, ProxyRequestDTO request) {
        log.info("[shadow] Starting async mirror for requestId={}", requestId);

        Instant v2Start = Instant.now();
        String v2Body = null;
        int v2StatusCode = 200;
        String v2Error = null;

        try {
            v2Body = callV2WithTimeout(request)
                    .block(Duration.ofMillis(shadowConfig.getV2TimeoutMs() + 1000)); // outer block slightly larger
        } catch (WebClientResponseException ex) {
            v2StatusCode = ex.getStatusCode().value();
            v2Body = ex.getResponseBodyAsString();
            v2Error = ex.getMessage();
            log.warn("[shadow] HTTP error {} from v2 for requestId={}", v2StatusCode, requestId);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("timeout")) {
                v2StatusCode = 504;
                v2Error = "TIMEOUT: " + ex.getMessage();
                log.warn("[shadow] Timeout calling v2 for requestId={}", requestId);
            } else {
                v2StatusCode = 500;
                v2Error = "UNEXPECTED: " + ex.getMessage();
                log.error("[shadow] Unexpected error for requestId={}: {}", requestId, ex.getMessage());
            }
        }

        long v2LatencyMs = Duration.between(v2Start, Instant.now()).toMillis();
        loggingService.saveResponse(requestId, "v2", v2StatusCode, v2Body, v2LatencyMs, v2Error);

        // Trigger comparison after both responses are persisted
        try {
            comparisonEngine.compare(requestId);
        } catch (Exception ex) {
            log.error("[shadow] Comparison failed for requestId={}: {}", requestId, ex.getMessage());
        }

        log.info("[shadow] Mirror complete for requestId={} v2Status={} v2Latency={}ms",
                requestId, v2StatusCode, v2LatencyMs);
    }

    private Mono<String> callV2WithTimeout(ProxyRequestDTO req) {
        HttpMethod method = HttpMethod.valueOf(
                req.getMethod() != null ? req.getMethod().toUpperCase() : "POST"
        );

        WebClient.RequestBodySpec spec = v2WebClient.method(method).uri(req.getEndpoint());

        if (req.getHeaders() != null) {
            req.getHeaders().forEach(spec::header);
        }

        return spec.bodyValue(req.getPayload() != null ? req.getPayload() : Map.of())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new WebClientResponseException(
                                                response.statusCode().value(),
                                                response.statusCode().toString(),
                                                null, body.getBytes(), null))))
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(shadowConfig.getV2TimeoutMs()));
    }
}
