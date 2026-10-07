package com.apisentinel.scanner;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.rules.SecurityRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecurityScanner {

    private final List<SecurityRule> rules;

    public SecurityScanner(List<SecurityRule> rules) {
        this.rules = rules;
    }

    public List<Finding> scan(ApiScan scan, List<ApiEndpointInfo> endpoints) {
        return endpoints.stream()
                .flatMap(endpoint -> rules.stream()
                        .map(rule -> rule.evaluate(scan, endpoint))
                        .flatMap(Optional::stream))
                .toList();
    }
}
