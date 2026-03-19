package com.shadowtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.config.ShadowConfig;
import com.shadowtool.dto.ProxyRequestDTO;
import com.shadowtool.dto.ProxyResponseDTO;
import com.shadowtool.entity.RequestEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
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
public class ProxyService {

    private final ShadowConfig shadowConfig;
    private final LoggingService loggingService;
    private final ShadowService shadowService;
    private final ObjectMapper objectMapper;
    private final WebClient v1WebClient;

    @Autowired
    public ProxyService(ShadowConfig shadowConfig,
                        LoggingService loggingService,
                        ShadowService shadowService,
                        ObjectMapper objectMapper,
                        @Qualifier("v1WebClient") WebClient v1WebClient) {
        this.shadowConfig = shadowConfig;
        this.loggingService = loggingService;
        this.shadowService = shadowService;
        this.objectMapper = objectMapper;
        this.v1WebClient = v1WebClient;
    }

    public ProxyResponseDTO proxy(ProxyRequestDTO request) {
        // 1. Persist the incoming request
        String payloadJson = toJson(request.getPayload());
        String headersJson = toJson(request.getHeaders());

        RequestEntity savedRequest = loggingService.saveRequest(
                request.getEndpoint(),
                request.getMethod() != null ? request.getMethod() : "POST",
                payloadJson,
                headersJson
        );
        UUID requestId = savedRequest.getId();

        // 2. Forward to v1 synchronously
        Instant v1Start = Instant.now();
        String v1Body = null;
        int v1StatusCode = 200;
        String v1Error = null;

        try {
            v1Body = callService(v1WebClient, request)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException ex) {
            v1StatusCode = ex.getStatusCode().value();
            v1Body = ex.getResponseBodyAsString();
            v1Error = ex.getMessage();
            log.warn("[v1] HTTP error {} for requestId={}", v1StatusCode, requestId);
        } catch (Exception ex) {
            v1StatusCode = 500;
            v1Error = ex.getMessage();
            log.error("[v1] Unexpected error for requestId={}: {}", requestId, ex.getMessage());
        }

        long v1LatencyMs = Duration.between(v1Start, Instant.now()).toMillis();
        loggingService.saveResponse(requestId, "v1", v1StatusCode, v1Body, v1LatencyMs, v1Error);

        // 3. Mirror to v2 asynchronously if configured
        boolean shadowTriggered = false;
        if (shadowConfig.shouldMirror()) {
            shadowTriggered = true;
            shadowService.mirrorAsync(requestId, request);
            log.info("[shadow] Mirror triggered for requestId={}", requestId);
        } else {
            log.debug("[shadow] Mirror skipped for requestId={} (disabled or sampling)", requestId);
        }

        // 4. Return v1 response to client immediately
        Object responseBody = parseJson(v1Body);
        return ProxyResponseDTO.builder()
                .requestId(requestId)
                .statusCode(v1StatusCode)
                .body(responseBody)
                .latencyMs(v1LatencyMs)
                .shadowTriggered(shadowTriggered)
                .build();
    }

    private Mono<String> callService(WebClient client, ProxyRequestDTO req) {
        HttpMethod method = HttpMethod.valueOf(
                req.getMethod() != null ? req.getMethod().toUpperCase() : "POST"
        );

        WebClient.RequestBodySpec spec = client.method(method).uri(req.getEndpoint());

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
                .bodyToMono(String.class);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return obj instanceof String s ? s : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
