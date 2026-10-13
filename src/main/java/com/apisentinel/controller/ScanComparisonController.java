package com.apisentinel.controller;

import com.apisentinel.dto.scan.ScanComparisonResponse;
import com.apisentinel.service.ScanComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scans")
public class ScanComparisonController {

    private final ScanComparisonService scanComparisonService;

    public ScanComparisonController(ScanComparisonService scanComparisonService) {
        this.scanComparisonService = scanComparisonService;
    }

    // Without baseScanId the scan is compared with the previous completed scan of the same project.
    @GetMapping("/{scanId}/comparison")
    public ScanComparisonResponse compare(
            @PathVariable Long scanId,
            @RequestParam(required = false) Long baseScanId
    ) {
        return scanComparisonService.compare(scanId, baseScanId);
    }
}
