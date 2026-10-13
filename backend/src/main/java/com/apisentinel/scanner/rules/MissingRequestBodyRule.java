package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class MissingRequestBodyRule implements SecurityRule {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH");

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        if (!WRITE_METHODS.contains(endpoint.getMethod()) || endpoint.hasRequestBody()) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "MISSING_REQUEST_BODY_SCHEMA",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.MEDIUM,
                "WEAK_INPUT_CONTRACT",
                "Write endpoint has no request body schema",
                "This write operation does not declare a request body schema in the OpenAPI specification.",
                "Define a request body schema so input validation expectations are clear and testable."
        ));
    }
}
