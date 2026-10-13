package com.apisentinel.scanner;

import com.apisentinel.exception.BadRequestException;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.model.OpenApiParseResult;
import com.apisentinel.scanner.model.ResponseShape;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class OpenApiParserService {

    public OpenApiParseResult parse(String content) {
        // Resolve $ref so that referenced parameters expose their name and location.
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            throw new BadRequestException("Uploaded file is not a valid OpenAPI specification");
        }

        List<String> serverUrls = extractServerUrls(openAPI);
        List<ApiEndpointInfo> endpoints = new ArrayList<>();
        if (openAPI.getPaths() == null) {
            return new OpenApiParseResult(endpoints, serverUrls);
        }

        boolean globalSecurity = openAPI.getSecurity() != null && !openAPI.getSecurity().isEmpty();
        openAPI.getPaths().forEach((path, pathItem) -> endpoints.addAll(extractEndpoints(openAPI, path, pathItem, globalSecurity)));
        return new OpenApiParseResult(endpoints, serverUrls);
    }

    private List<String> extractServerUrls(OpenAPI openAPI) {
        if (openAPI.getServers() == null) {
            return List.of();
        }

        return openAPI.getServers().stream()
                .map(Server::getUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ApiEndpointInfo> extractEndpoints(OpenAPI openAPI, String path, PathItem pathItem, boolean globalSecurity) {
        List<ApiEndpointInfo> endpoints = new ArrayList<>();

        for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
            Operation operation = entry.getValue();
            String method = entry.getKey().name().toUpperCase(Locale.ROOT);
            // Operation-level security overrides global security; "security: []" marks the operation as public.
            boolean secured = operation.getSecurity() != null
                    ? !operation.getSecurity().isEmpty()
                    : globalSecurity;
            boolean hasRequestBody = operation.getRequestBody() != null;
            List<String> queryParameters = extractQueryParameters(pathItem, operation);
            Set<String> responseCodes = operation.getResponses() != null
                    ? operation.getResponses().keySet()
                    : Set.of();
            ResponseShape responseShape = successResponseShape(openAPI, operation);
            endpoints.add(new ApiEndpointInfo(method, path, secured, hasRequestBody, queryParameters, responseCodes, responseShape));
        }

        return endpoints;
    }

    // Parameters can be declared on the path (shared by all operations) or on the operation itself.
    private List<String> extractQueryParameters(PathItem pathItem, Operation operation) {
        List<Parameter> pathParameters = pathItem.getParameters() != null ? pathItem.getParameters() : List.of();
        List<Parameter> operationParameters = operation.getParameters() != null ? operation.getParameters() : List.of();

        return Stream.concat(pathParameters.stream(), operationParameters.stream())
                .filter(parameter -> "query".equals(parameter.getIn()))
                .map(Parameter::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // Looks at the 200 response, or the first 2xx one, and follows $ref links into components.
    private ResponseShape successResponseShape(OpenAPI openAPI, Operation operation) {
        if (operation.getResponses() == null) {
            return ResponseShape.UNKNOWN;
        }
        ApiResponse response = operation.getResponses().get("200");
        if (response == null) {
            response = operation.getResponses().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("2"))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (response == null) {
            return ResponseShape.UNKNOWN;
        }

        Content content = response.getContent();
        if (content == null || content.isEmpty()) {
            return ResponseShape.UNKNOWN;
        }
        Schema<?> schema = resolve(openAPI, content.values().iterator().next().getSchema());
        if (schema == null) {
            return ResponseShape.UNKNOWN;
        }
        if (schema instanceof ArraySchema || "array".equals(schema.getType())
                || (schema.getTypes() != null && schema.getTypes().contains("array"))) {
            return ResponseShape.LIST;
        }
        if ("object".equals(schema.getType()) || schema.getProperties() != null || schema.getAllOf() != null) {
            return ResponseShape.OBJECT;
        }
        return ResponseShape.UNKNOWN;
    }

    private Schema<?> resolve(OpenAPI openAPI, Schema<?> schema) {
        // A few hops are enough for real specifications and stop reference cycles.
        for (int hop = 0; schema != null && schema.get$ref() != null && hop < 5; hop++) {
            String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            schema = openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null
                    ? openAPI.getComponents().getSchemas().get(name)
                    : null;
        }
        return schema;
    }
}
