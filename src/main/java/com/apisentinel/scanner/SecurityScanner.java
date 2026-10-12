package com.apisentinel.scanner;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.scanner.model.OpenApiParseResult;
import com.apisentinel.scanner.rules.ApiSpecRule;
import com.apisentinel.scanner.rules.SecurityRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SecurityScanner {

    private final List<SecurityRule> rules;
    private final List<ApiSpecRule> specRules;

    public SecurityScanner(List<SecurityRule> rules, List<ApiSpecRule> specRules) {
        this.rules = rules;
        this.specRules = specRules;
    }

    public List<Finding> scan(ApiScan scan, OpenApiParseResult spec) {
        Stream<Finding> specFindings = specRules.stream()
                .flatMap(rule -> rule.evaluate(scan, spec).stream());

        Stream<Finding> endpointFindings = spec.getEndpoints().stream()
                .flatMap(endpoint -> rules.stream()
                        .map(rule -> rule.evaluate(scan, endpoint))
                        .flatMap(Optional::stream));

        return Stream.concat(specFindings, endpointFindings).toList();
    }
}
