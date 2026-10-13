package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class SensitiveDataInQueryRule implements SecurityRule {

    // Compared against parameter names with case and separators removed, e.g. "api_key" -> "apikey".
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "secret", "token", "apikey", "creditcard", "cardnumber"
    );

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        List<String> sensitiveParameters = endpoint.getQueryParameters().stream()
                .filter(this::isSensitive)
                .toList();

        if (sensitiveParameters.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "SENSITIVE_DATA_IN_QUERY",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.HIGH,
                "SENSITIVE_DATA_EXPOSURE",
                "Sensitive data is sent in the URL",
                "Query parameters " + sensitiveParameters + " look sensitive. URLs are stored in browser history, proxy and server logs.",
                "Send credentials in the Authorization header and other sensitive values in the request body."
        ));
    }

    private boolean isSensitive(String parameterName) {
        String normalized = parameterName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return SENSITIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    }
}
