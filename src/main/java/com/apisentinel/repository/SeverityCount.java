package com.apisentinel.repository;

import com.apisentinel.entity.FindingSeverity;

// One row of the "findings per severity" query; Spring Data fills it from the query aliases.
public interface SeverityCount {

    FindingSeverity getSeverity();

    long getCount();
}
