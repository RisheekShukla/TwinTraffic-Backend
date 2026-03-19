package com.shadowtool.repository;

import com.shadowtool.entity.ResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponseRepository extends JpaRepository<ResponseEntity, UUID> {
    List<ResponseEntity> findByRequestId(UUID requestId);
    List<ResponseEntity> findByRequestIdAndSource(UUID requestId, String source);
}
