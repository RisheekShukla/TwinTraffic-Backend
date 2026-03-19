package com.shadowtool.service;

import com.shadowtool.entity.RequestEntity;
import com.shadowtool.entity.ResponseEntity;
import com.shadowtool.repository.RequestRepository;
import com.shadowtool.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;

    @Transactional
    public RequestEntity saveRequest(String endpoint, String method, String payload, String headers) {
        RequestEntity entity = RequestEntity.builder()
                .endpoint(endpoint)
                .method(method)
                .payload(payload)
                .headers(headers)
                .build();
        RequestEntity saved = requestRepository.save(entity);
        log.debug("[logging] Saved request id={} endpoint={}", saved.getId(), endpoint);
        return saved;
    }

    @Transactional
    public ResponseEntity saveResponse(UUID requestId, String source, int statusCode,
                                       String responseBody, long latencyMs, String errorMessage) {
        ResponseEntity entity = ResponseEntity.builder()
                .requestId(requestId)
                .source(source)
                .statusCode(statusCode)
                .responseBody(responseBody)
                .latencyMs(latencyMs)
                .errorMessage(errorMessage)
                .build();
        ResponseEntity saved = responseRepository.save(entity);
        log.debug("[logging] Saved {} response for requestId={} status={} latency={}ms",
                source, requestId, statusCode, latencyMs);
        return saved;
    }
}
