package com.apisentinel.repository;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiScanRepository extends JpaRepository<ApiScan, Long> {

    List<ApiScan> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // Ids are generated in insertion order, so the highest smaller id is the previous scan.
    Optional<ApiScan> findFirstByProjectIdAndStatusAndIdLessThanOrderByIdDesc(Long projectId, ScanStatus status, Long id);

    void deleteByProjectId(Long projectId);
}
