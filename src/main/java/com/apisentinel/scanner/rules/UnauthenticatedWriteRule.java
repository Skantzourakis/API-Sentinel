package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class UnauthenticatedWriteRule implements SecurityRule {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        if (!WRITE_METHODS.contains(endpoint.getMethod()) || endpoint.isSecured()) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "UNAUTHENTICATED_WRITE",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.HIGH,
                "MISSING_AUTHENTICATION",
                "Write endpoint has no declared security",
                "This operation creates, changes or deletes data, but the OpenAPI operation does not declare authentication.",
                "Require authentication for this operation and verify that the caller is allowed to modify the resource."
        ));
    }
}
