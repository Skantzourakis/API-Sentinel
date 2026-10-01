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

        openAPI.getPaths().forEach((path, pathItem) -> endpoints.addAll(extractEndpoints(path, pathItem)));
        return new OpenApiParseResult(endpoints);
    }

    private List<ApiEndpointInfo> extractEndpoints(String path, PathItem pathItem) {
        List<ApiEndpointInfo> endpoints = new ArrayList<>();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
            String method = entry.getKey().name().toUpperCase(Locale.ROOT);
            endpoints.add(new ApiEndpointInfo(method, path));
        }

        return endpoints;
    }
}
