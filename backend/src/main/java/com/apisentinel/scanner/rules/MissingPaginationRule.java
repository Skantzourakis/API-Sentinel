package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class MissingPaginationRule implements SecurityRule {

    // Compared against parameter names with case and separators removed, e.g. "page_size" -> "pagesize".
    private static final Set<String> PAGINATION_PARAMETERS = Set.of(
            "limit", "offset", "page", "size", "pagesize", "perpage", "cursor", "pagetoken", "maxresults", "top"
    );

    @Override
    public Optional<Finding> evaluate(ApiScan scan, ApiEndpointInfo endpoint) {
        // A path ending in a parameter such as /accounts/{id} returns a single item, not a list.
        boolean collectionRead = "GET".equals(endpoint.getMethod()) && !endpoint.getPath().endsWith("}");
        if (!collectionRead || hasPaginationParameter(endpoint)) {
            return Optional.empty();
        }

        return Optional.of(new Finding(
                scan,
                "MISSING_PAGINATION",
                endpoint.getMethod(),
                endpoint.getPath(),
                FindingSeverity.MEDIUM,
                "UNRESTRICTED_RESOURCE_CONSUMPTION",
                "Collection endpoint has no pagination parameters",
                "This endpoint appears to return a list but declares no limit, page or cursor parameter, so a single request may return an unbounded amount of data.",
                "Add pagination parameters such as limit and offset (or page and size) and enforce a maximum page size in the backend."
        ));
    }

    private boolean hasPaginationParameter(ApiEndpointInfo endpoint) {
        return endpoint.getQueryParameters().stream()
                .map(name -> name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""))
                .anyMatch(PAGINATION_PARAMETERS::contains);
    }
}
