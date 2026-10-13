package com.apisentinel.dto.project;

import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.entity.Project;

import java.time.Instant;
//Data Transfer Object
//ProjectResponse for Server-to-User communication.
//used for sending project data back to the client after processing requests.
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private long scanCount;
    // Null until the first file is uploaded.
    private ApiScanResponse latestScan;

    public static ProjectResponse from(Project project, long scanCount, ApiScanResponse latestScan) {
        ProjectResponse response = new ProjectResponse();
        response.id = project.getId();
        response.name = project.getName();
        response.description = project.getDescription();
        response.createdAt = project.getCreatedAt();
        response.updatedAt = project.getUpdatedAt();
        response.scanCount = scanCount;
        response.latestScan = latestScan;
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getScanCount() {
        return scanCount;
    }

    public ApiScanResponse getLatestScan() {
        return latestScan;
    }
}
