package com.shadowtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtool.dto.MetricsDTO;
import com.shadowtool.repository.ComparisonRepository;
import com.shadowtool.repository.RequestRepository;
import com.shadowtool.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;
    private final ComparisonRepository comparisonRepository;
    private final RedisTemplate<String, Long> redisTemplate;

    private static final String KEY_TOTAL = "metrics:total_requests";
    private static final String KEY_SHADOW = "metrics:total_shadow";

    public void incrementTotalRequests() {
        safeIncr(KEY_TOTAL);
    }

    public void incrementShadowRequests() {
        safeIncr(KEY_SHADOW);
    }

    public MetricsDTO getMetrics() {
        long totalRequests = requestRepository.count();
        long totalShadow  = responseRepository.findAll().stream()
                .filter(r -> "v2".equals(r.getSource())).count();

        long totalMismatches = comparisonRepository.countByMatchStatus("MISMATCH");
        long totalErrors     = comparisonRepository.countByMatchStatus("ERROR")
                             + comparisonRepository.countByMatchStatus("TIMEOUT");

        Double avgLatencyDiff = comparisonRepository.findAvgLatencyDiff();

        // Grouped breakdown
        List<Object[]> grouped = comparisonRepository.countByMatchStatusGrouped();
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            breakdown.put((String) row[0], (Long) row[1]);
        }

        double mismatchRate = totalShadow > 0
                ? (double) totalMismatches / totalShadow * 100.0 : 0.0;

        return MetricsDTO.builder()
                .totalRequests(totalRequests)
                .totalShadowRequests(totalShadow)
                .totalMismatches(totalMismatches)
                .totalErrors(totalErrors)
                .avgLatencyDiffMs(avgLatencyDiff)
                .matchStatusBreakdown(breakdown)
                .mismatchRatePercent(Math.round(mismatchRate * 100.0) / 100.0)
                .build();
    }

    private void safeIncr(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("[metrics] Redis unavailable, skipping counter increment for key={}", key);
        }
    }
}
