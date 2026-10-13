package com.apisentinel.service;

import com.apisentinel.dto.PageResponse;
import com.apisentinel.dto.finding.FindingResponse;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.dto.scan.ScanSummaryResponse;
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
import com.apisentinel.repository.SeverityCount;
import com.apisentinel.scanner.OpenApiParserService;
import com.apisentinel.scanner.SecurityScanner;
import com.apisentinel.scanner.model.OpenApiParseResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ScanService {

    private static final Set<String> ALLOWED_OPENAPI_EXTENSIONS = Set.of(".json", ".yaml", ".yml");
    private static final int MAX_PAGE_SIZE = 100;

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

    // noRollbackFor keeps the FAILED scan in the history even though the request returns 400.
    @Transactional(noRollbackFor = BadRequestException.class)
    public ApiScanResponse createScanFromUpload(Long ownerId, Long projectId, MultipartFile file) {
        Project project = findProject(ownerId, projectId);
        validateOpenApiFile(file);

        ApiScan scan = apiScanRepository.save(new ApiScan(project, normalizeFileName(file.getOriginalFilename())));

        OpenApiParseResult parseResult;
        try {
            parseResult = parseOpenApiFile(file);
        } catch (BadRequestException e) {
            scan.setStatus(ScanStatus.FAILED);
            scan.setCompletedAt(Instant.now());
            throw e;
        }

        scan.setEndpointCount(parseResult.getEndpointCount());

        List<Finding> findings = securityScanner.scan(scan, parseResult);
        scan.setRiskScore(calculateRiskScore(findings));
        scan.setStatus(ScanStatus.COMPLETED);
        scan.setCompletedAt(Instant.now());

        findingRepository.saveAll(findings);
        return ApiScanResponse.from(scan);
    }

    @Transactional(readOnly = true)
    public List<ApiScanResponse> getScansForProject(Long ownerId, Long projectId) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, ownerId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " was not found");
        }

        return apiScanRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(ApiScanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiScanResponse getScan(Long ownerId, Long scanId) {
        return ApiScanResponse.from(findScan(ownerId, scanId));
    }

    @Transactional(readOnly = true)
    public PageResponse<FindingResponse> getFindingsForScan(Long ownerId, Long scanId, FindingSeverity severity, int page, int size) {
        if (!apiScanRepository.existsByIdAndProjectOwnerId(scanId, ownerId)) {
            throw new ResourceNotFoundException("Scan with id " + scanId + " was not found");
        }
        if (page < 0) {
            throw new BadRequestException("Page must be 0 or greater");
        }
        // Capping the page size is what MissingPaginationRule asks other APIs to do.
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Finding> findings = severity == null
                ? findingRepository.findPageByScanId(scanId, pageable)
                : findingRepository.findByScanIdAndSeverityOrderByIdAsc(scanId, severity, pageable);

        return PageResponse.from(findings, FindingResponse::from);
    }

    @Transactional(readOnly = true)
    public ScanSummaryResponse getScanSummary(Long ownerId, Long scanId) {
        ApiScan scan = findScan(ownerId, scanId);

        // Start every severity at 0 so missing ones still appear, most severe first.
        Map<FindingSeverity, Long> findingsBySeverity = new LinkedHashMap<>();
        for (FindingSeverity severity : List.of(FindingSeverity.CRITICAL, FindingSeverity.HIGH, FindingSeverity.MEDIUM, FindingSeverity.LOW)) {
            findingsBySeverity.put(severity, 0L);
        }
        for (SeverityCount row : findingRepository.countBySeverityForScan(scanId)) {
            findingsBySeverity.put(row.getSeverity(), row.getCount());
        }

        return ScanSummaryResponse.from(scan, findingsBySeverity);
    }

    // Scans of other users' projects are reported as not found.
    private Project findProject(Long ownerId, Long projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + projectId + " was not found"));
    }

    private ApiScan findScan(Long ownerId, Long scanId) {
        return apiScanRepository.findByIdAndProjectOwnerId(scanId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan with id " + scanId + " was not found"));
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
