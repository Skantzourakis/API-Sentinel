package com.apisentinel.dto.finding;

import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;

public class FindingResponse {

    private Long id;
    private Long scanId;
    private String ruleId;
    private String method;
    private String path;
    private FindingSeverity severity;
    private String issueType;
    private String title;
    private String description;
    private String recommendation;

    public static FindingResponse from(Finding finding) {
        FindingResponse response = new FindingResponse();
        response.id = finding.getId();
        response.scanId = finding.getScan().getId();
        response.ruleId = finding.getRuleId();
        response.method = finding.getMethod();
        response.path = finding.getPath();
        response.severity = finding.getSeverity();
        response.issueType = finding.getIssueType();
        response.title = finding.getTitle();
        response.description = finding.getDescription();
        response.recommendation = finding.getRecommendation();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getScanId() {
        return scanId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
