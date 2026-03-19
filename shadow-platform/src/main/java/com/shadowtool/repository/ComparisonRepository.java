package com.shadowtool.repository;

import com.shadowtool.entity.ComparisonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComparisonRepository extends JpaRepository<ComparisonEntity, UUID> {
    Optional<ComparisonEntity> findByRequestId(UUID requestId);

    long countByMatchStatus(String matchStatus);

    @Query("SELECT AVG(c.latencyDiff) FROM ComparisonEntity c WHERE c.latencyDiff IS NOT NULL")
    Double findAvgLatencyDiff();

    @Query("SELECT c.matchStatus, COUNT(c) FROM ComparisonEntity c GROUP BY c.matchStatus")
    List<Object[]> countByMatchStatusGrouped();
}
