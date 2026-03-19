package com.shadowtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.dto.*;
import com.shadowtool.entity.ComparisonEntity;
import com.shadowtool.entity.RequestEntity;
import com.shadowtool.entity.ResponseEntity;
import com.shadowtool.repository.ComparisonRepository;
import com.shadowtool.repository.RequestRepository;
import com.shadowtool.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService {

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;
    private final ComparisonRepository comparisonRepository;
    private final ObjectMapper objectMapper;

    public Page<RequestLogDTO> listRequests(Pageable pageable) {
        return requestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toRequestLogDTO);
    }

    public RequestDetailDTO getDetail(UUID requestId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        List<ResponseEntity> responses = responseRepository.findByRequestId(requestId);
        Optional<ResponseEntity> v1 = responses.stream().filter(r -> "v1".equals(r.getSource())).findFirst();
        Optional<ResponseEntity> v2 = responses.stream().filter(r -> "v2".equals(r.getSource())).findFirst();
        Optional<ComparisonEntity> comparison = comparisonRepository.findByRequestId(requestId);

        return RequestDetailDTO.builder()
                .request(toRequestLogDTO(request))
                .v1Response(v1.map(this::toResponseLogDTO).orElse(null))
                .v2Response(v2.map(this::toResponseLogDTO).orElse(null))
                .comparison(comparison.map(this::toComparisonDTO).orElse(null))
                .build();
    }

    private RequestLogDTO toRequestLogDTO(RequestEntity e) {
        return RequestLogDTO.builder()
                .id(e.getId())
                .endpoint(e.getEndpoint())
                .method(e.getMethod())
                .payload(parseJson(e.getPayload()))
                .headers(parseJson(e.getHeaders()))
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ResponseLogDTO toResponseLogDTO(ResponseEntity e) {
        return ResponseLogDTO.builder()
                .id(e.getId())
                .requestId(e.getRequestId())
                .source(e.getSource())
                .statusCode(e.getStatusCode())
                .responseBody(parseJson(e.getResponseBody()))
                .latencyMs(e.getLatencyMs())
                .errorMessage(e.getErrorMessage())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ComparisonDTO toComparisonDTO(ComparisonEntity e) {
        return ComparisonDTO.builder()
                .id(e.getId())
                .requestId(e.getRequestId())
                .matchStatus(e.getMatchStatus())
                .latencyDiff(e.getLatencyDiff())
                .v1StatusCode(e.getV1StatusCode())
                .v2StatusCode(e.getV2StatusCode())
                .diffDetails(parseJson(e.getDiffDetails()))
                .createdAt(e.getCreatedAt())
                .build();
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
