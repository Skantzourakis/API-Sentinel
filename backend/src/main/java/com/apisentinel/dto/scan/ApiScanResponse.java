package com.apisentinel.dto.scan;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.entity.ScanStatus;

import java.time.Instant;
import java.util.Map;

public class ApiScanResponse {

    private Long id;
    private Long projectId;
    private ScanStatus status;
    private Integer riskScore;
    private Integer endpointCount;
    private String openApiFileName;
    private Instant createdAt;
    private Instant completedAt;
    private Map<FindingSeverity, Long> findingsBySeverity;

    public static ApiScanResponse from(ApiScan scan, Map<FindingSeverity, Long> findingsBySeverity) {
        ApiScanResponse response = new ApiScanResponse();
        response.id = scan.getId();
        response.projectId = scan.getProject().getId();
        response.status = scan.getStatus();
        response.riskScore = scan.getRiskScore();
        response.endpointCount = scan.getEndpointCount();
        response.openApiFileName = scan.getOpenApiFileName();
        response.createdAt = scan.getCreatedAt();
        response.completedAt = scan.getCompletedAt();
        response.findingsBySeverity = findingsBySeverity;
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public Integer getEndpointCount() {
        return endpointCount;
    }

    public String getOpenApiFileName() {
        return openApiFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Map<FindingSeverity, Long> getFindingsBySeverity() {
        return findingsBySeverity;
    }
}
