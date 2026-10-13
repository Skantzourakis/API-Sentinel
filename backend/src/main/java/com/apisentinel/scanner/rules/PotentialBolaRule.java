package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PotentialBolaRule implements SecurityRule {

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        boolean objectIdPath = endpoint.getPath().contains("{") && endpoint.getPath().contains("}");
        if (!objectIdPath || endpoint.isSecured()) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "POTENTIAL_BOLA",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.HIGH,
                "POTENTIAL_BOLA",
                "Object endpoint may need authorization review",
                "This endpoint exposes an object identifier in the path and has no declared security requirement.",
                "Verify that object-level authorization is enforced before returning or modifying this resource."
        ));
    }
}
