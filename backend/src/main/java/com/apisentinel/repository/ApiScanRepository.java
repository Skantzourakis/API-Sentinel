package com.apisentinel.repository;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiScanRepository extends JpaRepository<ApiScan, Long> {

    List<ApiScan> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<ApiScan> findByIdAndProjectOwnerId(Long id, Long ownerId);

    boolean existsByIdAndProjectOwnerId(Long id, Long ownerId);

    // Ids are generated in insertion order, so the highest smaller id is the previous scan.
    Optional<ApiScan> findFirstByProjectIdAndStatusAndIdLessThanOrderByIdDesc(Long projectId, ScanStatus status, Long id);

    void deleteByProjectId(Long projectId);

    long countByProjectId(Long projectId);

    Optional<ApiScan> findFirstByProjectIdOrderByIdDesc(Long projectId);

    // The newest scan of every project the user owns, in one query.
    @Query("""
            SELECT s FROM ApiScan s
            WHERE s.project.owner.id = :ownerId
              AND s.id = (SELECT MAX(s2.id) FROM ApiScan s2 WHERE s2.project = s.project)
            """)
    List<ApiScan> findLatestScanPerProjectOfOwner(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT s.project.id AS projectId, COUNT(s) AS count
            FROM ApiScan s
            WHERE s.project.owner.id = :ownerId
            GROUP BY s.project.id
            """)
    List<ProjectScanCount> countScansPerProjectOfOwner(@Param("ownerId") Long ownerId);
}
