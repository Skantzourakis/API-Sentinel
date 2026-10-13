package com.apisentinel.dto.scan;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.entity.ScanStatus;

import java.util.Map;

public class ScanSummaryResponse {

    private Long scanId;
    private ScanStatus status;
    private Integer riskScore;
    private Integer endpointCount;
    private long totalFindings;
    private Map<FindingSeverity, Long> findingsBySeverity;

    public static ScanSummaryResponse from(ApiScan scan, Map<FindingSeverity, Long> findingsBySeverity) {
        ScanSummaryResponse response = new ScanSummaryResponse();
        response.scanId = scan.getId();
        response.status = scan.getStatus();
        response.riskScore = scan.getRiskScore();
        response.endpointCount = scan.getEndpointCount();
        response.totalFindings = findingsBySeverity.values().stream().mapToLong(Long::longValue).sum();
        response.findingsBySeverity = findingsBySeverity;
        return response;
    }

    public Long getScanId() {
        return scanId;
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

    public long getTotalFindings() {
        return totalFindings;
    }

    public Map<FindingSeverity, Long> getFindingsBySeverity() {
        return findingsBySeverity;
    }
}
