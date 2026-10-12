package com.apisentinel.scanner.rules;

import com.apisentinel.entity.ApiScan;
import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.OpenApiParseResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class InsecureServerUrlRule implements ApiSpecRule {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0");

    @Override
    public List<Finding> evaluate(ApiScan scan, OpenApiParseResult spec) {
        return spec.getServerUrls().stream()
                .filter(this::isInsecureRemoteUrl)
                .map(url -> new Finding(
                        scan,
                        "INSECURE_SERVER_URL",
                        "ALL",
                        url,
                        FindingSeverity.HIGH,
                        "SECURITY_MISCONFIGURATION",
                        "Server URL does not use HTTPS",
                        "The API is declared on a plain HTTP server, so requests, tokens and responses travel unencrypted.",
                        "Serve the API only over HTTPS and update the servers section of the specification."
                ))
                .toList();
    }

    // Plain HTTP on the developer's own machine is common and not exposed to the network.
    private boolean isInsecureRemoteUrl(String url) {
        if (!url.toLowerCase(Locale.ROOT).startsWith("http://")) {
            return false;
        }

        try {
            String host = URI.create(url).getHost();
            return host == null || !LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
