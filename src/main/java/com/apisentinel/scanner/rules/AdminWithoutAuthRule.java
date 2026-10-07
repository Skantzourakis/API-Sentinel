package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class AdminWithoutAuthRule implements SecurityRule {

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        boolean adminPath = endpoint.getPath().toLowerCase(Locale.ROOT).contains("/admin");
        if (!adminPath || endpoint.isSecured()) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "ADMIN_WITHOUT_AUTH",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.CRITICAL,
                "MISSING_AUTHENTICATION",
                "Admin endpoint has no declared security",
                "This endpoint appears to be administrative, but the OpenAPI operation does not declare authentication.",
                "Add a security requirement to this operation and enforce authorization in the backend."
        ));
    }
}
