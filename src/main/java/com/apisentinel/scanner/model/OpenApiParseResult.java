package com.apisentinel.scanner.model;

import java.util.List;

public class OpenApiParseResult {

    private final List<ApiEndpointInfo> endpoints;

    public OpenApiParseResult(List<ApiEndpointInfo> endpoints) {
        this.endpoints = endpoints;
    }

    public List<ApiEndpointInfo> getEndpoints() {
        return endpoints;
    }

    public int getEndpointCount() {
        return endpoints.size();
    }
}
