package com.apisentinel.repository;

import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    // Most severe first; JPQL refers to enum constants by their fully qualified name.
    @Query(value = """
            SELECT f FROM Finding f
            WHERE f.scan.id = :scanId
            ORDER BY CASE f.severity
                WHEN com.apisentinel.entity.FindingSeverity.CRITICAL THEN 0
                WHEN com.apisentinel.entity.FindingSeverity.HIGH THEN 1
                WHEN com.apisentinel.entity.FindingSeverity.MEDIUM THEN 2
                ELSE 3
            END, f.id
            """,
            countQuery = "SELECT COUNT(f) FROM Finding f WHERE f.scan.id = :scanId")
    Page<Finding> findPageByScanId(@Param("scanId") Long scanId, Pageable pageable);

    Page<Finding> findByScanIdAndSeverityOrderByIdAsc(Long scanId, FindingSeverity severity, Pageable pageable);

    @Query("""
            SELECT f.severity AS severity, COUNT(f) AS count
            FROM Finding f
            WHERE f.scan.id = :scanId
            GROUP BY f.severity
            """)
    List<SeverityCount> countBySeverityForScan(@Param("scanId") Long scanId);

    void deleteByScanProjectId(Long projectId);
}
