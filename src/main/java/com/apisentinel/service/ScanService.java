package com.apisentinel.service;

import com.apisentinel.dto.scan.ApiScanRequest;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Project;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ApiScanRepository;
import com.apisentinel.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScanService {

    private final ApiScanRepository apiScanRepository;
    private final ProjectRepository projectRepository;

    public ScanService(ApiScanRepository apiScanRepository, ProjectRepository projectRepository) {
        this.apiScanRepository = apiScanRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ApiScanResponse createScan(Long projectId, ApiScanRequest request) {
        Project project = findProject(projectId);
        ApiScan scan = new ApiScan(project, normalizeFileName(request.getOpenApiFileName()));
        ApiScan savedScan = apiScanRepository.save(scan);
        return ApiScanResponse.from(savedScan);
    }

    @Transactional(readOnly = true)
    public List<ApiScanResponse> getScansForProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " was not found");
        }

        return apiScanRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(ApiScanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiScanResponse getScan(Long scanId) {
        ApiScan scan = apiScanRepository.findById(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan with id " + scanId + " was not found"));
        return ApiScanResponse.from(scan);
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + projectId + " was not found"));
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return fileName.trim();
    }
}
