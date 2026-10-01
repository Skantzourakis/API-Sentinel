package com.apisentinel.repository;

import com.apisentinel.entity.ApiScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiScanRepository extends JpaRepository<ApiScan, Long> {

    List<ApiScan> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
