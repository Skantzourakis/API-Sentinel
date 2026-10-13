package com.apisentinel.repository;

import com.apisentinel.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByScanId(Long scanId);

    @Query("""
            SELECT f.severity AS severity, COUNT(f) AS count
            FROM Finding f
            WHERE f.scan.id = :scanId
            GROUP BY f.severity
            """)
    List<SeverityCount> countBySeverityForScan(@Param("scanId") Long scanId);

    void deleteByScanProjectId(Long projectId);
}
