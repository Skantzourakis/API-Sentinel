package com.apisentinel.scanner;

import com.apisentinel.exception.BadRequestException;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.model.OpenApiParseResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenApiParserService {

    public OpenApiParseResult parse(String content) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            throw new BadRequestException("Uploaded file is not a valid OpenAPI specification");
        }

        List<ApiEndpointInfo> endpoints = new ArrayList<>();
        if (openAPI.getPaths() == null) {
            return new OpenApiParseResult(endpoints);
        }

        boolean globalSecurity = openAPI.getSecurity() != null && !openAPI.getSecurity().isEmpty();
        openAPI.getPaths().forEach((path, pathItem) -> endpoints.addAll(extractEndpoints(path, pathItem, globalSecurity)));
        return new OpenApiParseResult(endpoints);
    }

    private List<ApiEndpointInfo> extractEndpoints(String path, PathItem pathItem, boolean globalSecurity) {
        List<ApiEndpointInfo> endpoints = new ArrayList<>();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
            Operation operation = entry.getValue();
            String method = entry.getKey().name().toUpperCase(Locale.ROOT);
            // Operation-level security overrides global security; "security: []" marks the operation as public.
            boolean secured = operation.getSecurity() != null
                    ? !operation.getSecurity().isEmpty()
                    : globalSecurity;
            boolean hasRequestBody = operation.getRequestBody() != null;
            endpoints.add(new ApiEndpointInfo(method, path, secured, hasRequestBody));
        }

        return endpoints;
    }
}
