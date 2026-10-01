package com.apisentinel.scanner.model;

public class ApiEndpointInfo {

    private final String method;
    private final String path;

    public ApiEndpointInfo(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
