package com.apisentinel.repository;

// One row of the "scans per project" query.
public interface ProjectScanCount {

    Long getProjectId();

    long getCount();
}
