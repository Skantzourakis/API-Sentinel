package com.apisentinel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "findings")
public class Finding {

    private static final int PATH_LENGTH = 500;
    private static final int DESCRIPTION_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private ApiScan scan;

    @Column(nullable = false, length = 80)
    private String ruleId;

    @Column(nullable = false, length = 20)
    private String method;

    @Column(nullable = false, length = PATH_LENGTH)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingSeverity severity;

    @Column(nullable = false, length = 80)
    private String issueType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = DESCRIPTION_LENGTH)
    private String description;

    @Column(nullable = false, length = 1000)
    private String recommendation;

    protected Finding() {
    }

    public Finding(
            ApiScan scan,
            String ruleId,
            String method,
            String path,
            FindingSeverity severity,
            String issueType,
            String title,
            String description,
            String recommendation
    ) {
        this.scan = scan;
        this.ruleId = ruleId;
        this.method = method;
        // The path and the description can contain text from the uploaded file (paths, server URLs, parameter names).
        this.path = ColumnText.fit(path, PATH_LENGTH);
        this.severity = severity;
        this.issueType = issueType;
        this.title = title;
        this.description = ColumnText.fit(description, DESCRIPTION_LENGTH);
        this.recommendation = recommendation;
    }

    public Long getId() {
        return id;
    }

    public ApiScan getScan() {
        return scan;
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
