package com.apisentinel.dto.scan;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.ScanStatus;

import java.time.Instant;

public class ApiScanResponse {

    private Long id;
    private Long projectId;
    private ScanStatus status;
    private Integer riskScore;
    private String openApiFileName;
    private Instant createdAt;
    private Instant completedAt;

    public static ApiScanResponse from(ApiScan scan) {
        ApiScanResponse response = new ApiScanResponse();
        response.id = scan.getId();
        response.projectId = scan.getProject().getId();
        response.status = scan.getStatus();
        response.riskScore = scan.getRiskScore();
        response.openApiFileName = scan.getOpenApiFileName();
        response.createdAt = scan.getCreatedAt();
        response.completedAt = scan.getCompletedAt();
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

    public String getOpenApiFileName() {
        return openApiFileName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
