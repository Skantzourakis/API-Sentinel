package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.scanner.model.OpenApiParseResult;

import java.util.List;

// Checks settings of the whole specification (such as servers) rather than a single endpoint.
public interface ApiSpecRule {

    List<Finding> evaluate(ApiScan scan, OpenApiParseResult spec);
}
