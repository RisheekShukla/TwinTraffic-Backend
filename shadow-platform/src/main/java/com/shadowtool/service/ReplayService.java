package com.shadowtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.dto.ProxyRequestDTO;
import com.shadowtool.dto.ProxyResponseDTO;
import com.shadowtool.entity.RequestEntity;
import com.shadowtool.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayService {

    private final RequestRepository requestRepository;
    private final ProxyService proxyService;
    private final ObjectMapper objectMapper;

    public ProxyResponseDTO replay(UUID requestId) {
        RequestEntity original = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        log.info("[replay] Replaying requestId={} endpoint={}", requestId, original.getEndpoint());

        // Reconstruct ProxyRequestDTO from stored entity
        ProxyRequestDTO replayRequest = ProxyRequestDTO.builder()
                .endpoint(original.getEndpoint())
                .method(original.getMethod())
                .payload(parseJson(original.getPayload()))
                .headers(parseHeaders(original.getHeaders()))
                .build();

        return proxyService.proxy(replayRequest);
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, String> parseHeaders(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, java.util.Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
