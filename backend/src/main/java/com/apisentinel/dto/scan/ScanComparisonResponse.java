package com.apisentinel.dto.scan;

import com.apisentinel.dto.finding.FindingResponse;
import com.apisentinel.entity.ApiScan;

import java.util.List;

public class ScanComparisonResponse {

    private Long scanId;
    private Long baseScanId;
    private Integer baseRiskScore;
    private Integer riskScore;
    private int riskScoreChange;
    private List<FindingResponse> newFindings;
    private List<FindingResponse> resolvedFindings;
    private int unchangedFindings;

    public static ScanComparisonResponse from(
            ApiScan scan,
            ApiScan baseScan,
            List<FindingResponse> newFindings,
            List<FindingResponse> resolvedFindings,
            int unchangedFindings
    ) {
        ScanComparisonResponse response = new ScanComparisonResponse();
        response.scanId = scan.getId();
        response.baseScanId = baseScan.getId();
        response.baseRiskScore = baseScan.getRiskScore();
        response.riskScore = scan.getRiskScore();
        response.riskScoreChange = scan.getRiskScore() - baseScan.getRiskScore();
        response.newFindings = newFindings;
        response.resolvedFindings = resolvedFindings;
        response.unchangedFindings = unchangedFindings;
        return response;
    }

    public Long getScanId() {
        return scanId;
    }

    public Long getBaseScanId() {
        return baseScanId;
    }

    public Integer getBaseRiskScore() {
        return baseRiskScore;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public int getRiskScoreChange() {
        return riskScoreChange;
    }

    public List<FindingResponse> getNewFindings() {
        return newFindings;
    }

    public List<FindingResponse> getResolvedFindings() {
        return resolvedFindings;
    }

    public int getUnchangedFindings() {
        return unchangedFindings;
    }
}
