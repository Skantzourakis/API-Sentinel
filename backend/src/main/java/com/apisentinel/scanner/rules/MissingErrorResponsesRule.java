package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MissingErrorResponsesRule implements SecurityRule {

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        boolean declaresClientErrors = endpoint.getResponseCodes().stream()
                .anyMatch(code -> code.startsWith("4") || code.equalsIgnoreCase("default"));
        if (declaresClientErrors) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "MISSING_ERROR_RESPONSES",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.LOW,
                "WEAK_ERROR_CONTRACT",
                "Endpoint declares no error responses",
                "This operation documents no 4xx or default response, so clients cannot tell how invalid input or failed authorization is reported.",
                "Document the expected error responses (for example 400, 401, 403 and 404) with a consistent error body."
        ));
    }
}
