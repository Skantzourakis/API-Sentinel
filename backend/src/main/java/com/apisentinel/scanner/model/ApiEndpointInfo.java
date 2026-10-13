package com.apisentinel.scanner.model;

import java.util.List;
import java.util.Set;

public class ApiEndpointInfo {

    private final String method;
    private final String path;
    private final boolean secured;
    private final boolean hasRequestBody;
    private final List<String> queryParameters;
    private final Set<String> responseCodes;
    private final ResponseShape responseShape;

    public ApiEndpointInfo(
            String method,
            String path,
            boolean secured,
            boolean hasRequestBody,
            List<String> queryParameters,
            Set<String> responseCodes,
            ResponseShape responseShape
    ) {
        this.method = method;
        this.path = path;
        this.secured = secured;
        this.hasRequestBody = hasRequestBody;
        this.queryParameters = List.copyOf(queryParameters);
        this.responseCodes = Set.copyOf(responseCodes);
        this.responseShape = responseShape;
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

    public List<String> getQueryParameters() {
        return queryParameters;
    }

    public Set<String> getResponseCodes() {
        return responseCodes;
    }

    public ResponseShape getResponseShape() {
        return responseShape;
    }
}
