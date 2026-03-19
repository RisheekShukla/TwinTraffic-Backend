package com.shadowtool.repository;

import com.shadowtool.entity.RequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<RequestEntity, UUID> {
    Page<RequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
