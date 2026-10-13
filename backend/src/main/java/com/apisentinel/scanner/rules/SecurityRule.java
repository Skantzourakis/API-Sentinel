package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.scanner.model.ApiEndpointInfo;

import java.util.Optional;

public interface SecurityRule {

    Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint);
}
