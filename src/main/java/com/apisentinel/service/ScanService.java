package com.apisentinel.service;

import com.apisentinel.dto.finding.FindingResponse;
import com.apisentinel.dto.scan.ApiScanRequest;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.entity.Project;
import com.apisentinel.entity.ScanStatus;
import com.apisentinel.exception.BadRequestException;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.ApiScanRepository;
import com.apisentinel.repository.FindingRepository;
import com.apisentinel.repository.ProjectRepository;
import com.apisentinel.scanner.OpenApiParserService;
import com.apisentinel.scanner.SecurityScanner;
import com.apisentinel.scanner.model.OpenApiParseResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ScanService {

    private static final Set<String> ALLOWED_OPENAPI_EXTENSIONS = Set.of(".json", ".yaml", ".yml");

    private final ApiScanRepository apiScanRepository;
    private final FindingRepository findingRepository;
    private final ProjectRepository projectRepository;
    private final OpenApiParserService openApiParserService;
    private final SecurityScanner securityScanner;

    public ScanService(
            ApiScanRepository apiScanRepository,
            FindingRepository findingRepository,
            ProjectRepository projectRepository,
            OpenApiParserService openApiParserService,
            SecurityScanner securityScanner
    ) {
        this.apiScanRepository = apiScanRepository;
        this.findingRepository = findingRepository;
        this.projectRepository = projectRepository;
        this.openApiParserService = openApiParserService;
        this.securityScanner = securityScanner;
    }

    @Transactional
    public ApiScanResponse createScan(Long projectId, ApiScanRequest request) {
        Project project = findProject(projectId);
        ApiScan scan = new ApiScan(project, normalizeFileName(request.getOpenApiFileName()));
        ApiScan savedScan = apiScanRepository.save(scan);
        return ApiScanResponse.from(savedScan);
    }

    @Transactional
    public ApiScanResponse createScanFromUpload(Long projectId, MultipartFile file) {
        Project project = findProject(projectId);
        validateOpenApiFile(file);

        OpenApiParseResult parseResult = parseOpenApiFile(file);
        ApiScan scan = new ApiScan(project, normalizeFileName(file.getOriginalFilename()));
        scan.setEndpointCount(parseResult.getEndpointCount());

        List<Finding> findings = securityScanner.scan(scan, parseResult.getEndpoints());
        scan.setRiskScore(calculateRiskScore(findings));
        scan.setStatus(ScanStatus.COMPLETED);
        scan.setCompletedAt(Instant.now());

        ApiScan savedScan = apiScanRepository.save(scan);
        findingRepository.saveAll(findings);
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

    @Transactional(readOnly = true)
    public List<FindingResponse> getFindingsForScan(Long scanId) {
        if (!apiScanRepository.existsById(scanId)) {
            throw new ResourceNotFoundException("Scan with id " + scanId + " was not found");
        }

        return findingRepository.findByScanId(scanId)
                .stream()
                .map(FindingResponse::from)
                .toList();
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

    private void validateOpenApiFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("OpenAPI file is required");
        }

        String fileName = normalizeFileName(file.getOriginalFilename());
        if (fileName == null) {
            throw new BadRequestException("OpenAPI file name is required");
        }

        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
        boolean allowedExtension = ALLOWED_OPENAPI_EXTENSIONS.stream()
                .anyMatch(lowerCaseFileName::endsWith);

        if (!allowedExtension) {
            throw new BadRequestException("Only .json, .yaml, and .yml OpenAPI files are supported");
        }
    }

    private OpenApiParseResult parseOpenApiFile(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return openApiParserService.parse(content);
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded OpenAPI file");
        }
    }

    private int calculateRiskScore(List<Finding> findings) {
        int score = findings.stream()
                .map(Finding::getSeverity)
                .mapToInt(this::severityWeight)
                .sum();
        return Math.min(score, 100);
    }

    private int severityWeight(FindingSeverity severity) {
        return switch (severity) {
            case LOW -> 5;
            case MEDIUM -> 15;
            case HIGH -> 30;
            case CRITICAL -> 45;
        };
    }
}
