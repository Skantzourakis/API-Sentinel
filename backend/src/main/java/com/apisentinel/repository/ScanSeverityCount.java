package com.apisentinel.repository;

import com.apisentinel.entity.FindingSeverity;

// One row of the "findings per scan and severity" query for a whole project.
public interface ScanSeverityCount {

    Long getScanId();

    FindingSeverity getSeverity();

    long getCount();
}
