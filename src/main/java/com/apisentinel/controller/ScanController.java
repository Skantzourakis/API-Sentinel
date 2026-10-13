package com.apisentinel.controller;

import com.apisentinel.dto.PageResponse;
import com.apisentinel.dto.finding.FindingResponse;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.dto.scan.ScanSummaryResponse;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.security.CurrentUser;
import com.apisentinel.service.ScanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping(value = "/projects/{projectId}/scans/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiScanResponse uploadOpenApiFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file
    ) {
        return scanService.createScanFromUpload(CurrentUser.idOf(jwt), projectId, file);
    }

    @GetMapping("/projects/{projectId}/scans")
    public List<ApiScanResponse> getScansForProject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projectId) {
        return scanService.getScansForProject(CurrentUser.idOf(jwt), projectId);
    }

    @GetMapping("/scans/{scanId}")
    public ApiScanResponse getScan(@AuthenticationPrincipal Jwt jwt, @PathVariable Long scanId) {
        return scanService.getScan(CurrentUser.idOf(jwt), scanId);
    }

    @GetMapping("/scans/{scanId}/findings")
    public PageResponse<FindingResponse> getFindingsForScan(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long scanId,
            @RequestParam(required = false) FindingSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return scanService.getFindingsForScan(CurrentUser.idOf(jwt), scanId, severity, page, size);
    }

    @GetMapping("/scans/{scanId}/summary")
    public ScanSummaryResponse getScanSummary(@AuthenticationPrincipal Jwt jwt, @PathVariable Long scanId) {
        return scanService.getScanSummary(CurrentUser.idOf(jwt), scanId);
    }
}
