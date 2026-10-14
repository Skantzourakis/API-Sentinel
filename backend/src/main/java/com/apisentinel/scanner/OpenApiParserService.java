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
import io.swagger.v3.parser.util.DeserializationUtils;
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

    private static final String SCHEMAS = "#/components/schemas/";
    private static final String PARAMETERS = "#/components/parameters/";
    private static final String RESPONSES = "#/components/responses/";

    static {
        // The parser allows 10 million YAML alias expansions by default, so a 557-byte
        // "billion laughs" upload kept a CPU busy for seconds. Real specifications need far fewer.
        DeserializationUtils.getOptions().setMaxYamlReferences(100_000L);
    }

    public OpenApiParseResult parse(String content) {
        // Never let the parser resolve $ref itself: it would fetch any URL an uploaded file names
        // (server-side request forgery). Only references inside the document are followed, below.
        ParseOptions options = new ParseOptions();
        options.setResolve(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            throw new BadRequestException("Uploaded file is not a valid OpenAPI specification");
        }
        boolean exceededLimits = result.getMessages() != null
                && result.getMessages().stream().anyMatch(message -> message.contains("exceeds max"));
        if (exceededLimits) {
            throw new BadRequestException("Uploaded file is too complex to analyse safely");
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
            List<String> queryParameters = extractQueryParameters(openAPI, pathItem, operation);
            Set<String> responseCodes = operation.getResponses() != null
                    ? operation.getResponses().keySet()
                    : Set.of();
            ResponseShape responseShape = successResponseShape(openAPI, operation);
            endpoints.add(new ApiEndpointInfo(method, path, secured, hasRequestBody, queryParameters, responseCodes, responseShape));
        }

        return endpoints;
    }

    // Parameters can be declared on the path (shared by all operations) or on the operation itself.
    private List<String> extractQueryParameters(OpenAPI openAPI, PathItem pathItem, Operation operation) {
        List<Parameter> pathParameters = pathItem.getParameters() != null ? pathItem.getParameters() : List.of();
        List<Parameter> operationParameters = operation.getParameters() != null ? operation.getParameters() : List.of();

        return Stream.concat(pathParameters.stream(), operationParameters.stream())
                .map(parameter -> resolveParameter(openAPI, parameter))
                .filter(Objects::nonNull)
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
        response = resolveResponse(openAPI, response);
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
            String name = componentName(schema.get$ref(), SCHEMAS);
            schema = name != null && openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null
                    ? openAPI.getComponents().getSchemas().get(name)
                    : null;
        }
        return schema;
    }

    private Parameter resolveParameter(OpenAPI openAPI, Parameter parameter) {
        if (parameter.get$ref() == null) {
            return parameter;
        }
        String name = componentName(parameter.get$ref(), PARAMETERS);
        return name != null && openAPI.getComponents() != null && openAPI.getComponents().getParameters() != null
                ? openAPI.getComponents().getParameters().get(name)
                : null;
    }

    private ApiResponse resolveResponse(OpenAPI openAPI, ApiResponse response) {
        if (response == null || response.get$ref() == null) {
            return response;
        }
        String name = componentName(response.get$ref(), RESPONSES);
        return name != null && openAPI.getComponents() != null && openAPI.getComponents().getResponses() != null
                ? openAPI.getComponents().getResponses().get(name)
                : null;
    }

    // Only references into this document's components are followed; anything else (URLs, files) is ignored.
    private String componentName(String ref, String prefix) {
        return ref.startsWith(prefix) ? ref.substring(prefix.length()) : null;
    }
}
