package com.apisentinel.controller;

import com.apisentinel.dto.scan.ApiScanRequest;
import com.apisentinel.dto.scan.ApiScanResponse;
import com.apisentinel.service.ScanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/projects/{projectId}/scans")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiScanResponse createScan(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiScanRequest request
    ) {
        return scanService.createScan(projectId, request);
    }

    @GetMapping("/projects/{projectId}/scans")
    public List<ApiScanResponse> getScansForProject(@PathVariable Long projectId) {
        return scanService.getScansForProject(projectId);
    }

    @GetMapping("/scans/{scanId}")
    public ApiScanResponse getScan(@PathVariable Long scanId) {
        return scanService.getScan(scanId);
    }
}
