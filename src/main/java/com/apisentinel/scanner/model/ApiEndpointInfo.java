package com.apisentinel.scanner.model;

public class ApiEndpointInfo {

    private final String method;
    private final String path;
    private final boolean secured;
    private final boolean hasRequestBody;

    public ApiEndpointInfo(String method, String path, boolean secured, boolean hasRequestBody) {
        this.method = method;
        this.path = path;
        this.secured = secured;
        this.hasRequestBody = hasRequestBody;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isSecured() {
        return secured;
    }

    public boolean hasRequestBody() {
        return hasRequestBody;
    }
}
