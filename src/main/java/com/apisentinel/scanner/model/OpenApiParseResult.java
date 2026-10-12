package com.apisentinel.scanner.model;

import java.util.List;

public class OpenApiParseResult {

    private final List<ApiEndpointInfo> endpoints;
    private final List<String> serverUrls;

    public OpenApiParseResult(List<ApiEndpointInfo> endpoints, List<String> serverUrls) {
        this.endpoints = endpoints;
        this.serverUrls = List.copyOf(serverUrls);
    }

    public List<ApiEndpointInfo> getEndpoints() {
        return endpoints;
    }

    public List<String> getServerUrls() {
        return serverUrls;
    }

    public int getEndpointCount() {
        return endpoints.size();
    }
}
