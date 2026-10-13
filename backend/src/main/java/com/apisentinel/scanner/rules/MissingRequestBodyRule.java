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
        // Writes that take their input as query parameters, such as PUT /me/player/pause?device_id=, need no body.
        boolean declaresInput = endpoint.hasRequestBody() || !endpoint.getQueryParameters().isEmpty();
        if (!WRITE_METHODS.contains(endpoint.getMethod()) || declaresInput) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "MISSING_REQUEST_BODY_SCHEMA",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.MEDIUM,
                "WEAK_INPUT_CONTRACT",
                "Write endpoint declares no input",
                "This write operation declares neither a request body nor parameters, so the input it accepts is undocumented.",
                "Define a request body schema so input validation expectations are clear and testable."
        ));
    }
}
