package com.apisentinel.scanner;

import com.apisentinel.exception.BadRequestException;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.model.OpenApiParseResult;
import com.apisentinel.scanner.model.ResponseShape;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class OpenApiParserServiceTest {

    private final OpenApiParserService parser = new OpenApiParserService();

    private static final String SPEC = """
            openapi: 3.0.0
            info:
              title: Test
              version: 1.0.0
            servers:
              - url: http://api.example.com
            security:
              - bearerAuth: []
            components:
              securitySchemes:
                bearerAuth:
                  type: http
                  scheme: bearer
              schemas:
                User:
                  type: object
                  properties:
                    email:
                      type: string
            paths:
              /accounts:
                parameters:
                  - name: tenant
                    in: query
                    schema:
                      type: string
                get:
                  parameters:
                    - name: limit
                      in: query
                      schema:
                        type: integer
                    - name: X-Trace
                      in: header
                      schema:
                        type: string
                  responses:
                    "200":
                      description: OK
                      content:
                        application/json:
                          schema:
                            type: array
                            items:
                              type: object
                    "401":
                      description: Unauthorized
              /me:
                get:
                  responses:
                    "200":
                      description: OK
                      content:
                        application/json:
                          schema:
                            $ref: "#/components/schemas/User"
              /health:
                get:
                  security: []
                  responses:
                    "200":
                      description: OK
              /payments:
                post:
                  requestBody:
                    content:
                      application/json:
                        schema:
                          type: object
                  responses:
                    "201":
                      description: Created
            """;

    private ApiEndpointInfo endpoint(OpenApiParseResult result, String method, String path) {
        return result.getEndpoints().stream()
                .filter(e -> e.getMethod().equals(method) && e.getPath().equals(path))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void extractsEveryOperationAndTheServers() {
        OpenApiParseResult result = parser.parse(SPEC);

        assertThat(result.getEndpointCount()).isEqualTo(4);
        assertThat(result.getServerUrls()).containsExactly("http://api.example.com");
    }

    @Test
    void globalSecurityAppliesUnlessTheOperationOverridesIt() {
        OpenApiParseResult result = parser.parse(SPEC);

        assertThat(endpoint(result, "GET", "/accounts").isSecured()).isTrue();
        assertThat(endpoint(result, "POST", "/payments").isSecured()).isTrue();
        // "security: []" makes the operation public even though global security is declared.
        assertThat(endpoint(result, "GET", "/health").isSecured()).isFalse();
    }

    @Test
    void collectsQueryParametersFromPathAndOperationOnly() {
        ApiEndpointInfo accounts = endpoint(parser.parse(SPEC), "GET", "/accounts");

        assertThat(accounts.getQueryParameters()).containsExactlyInAnyOrder("tenant", "limit");
    }

    @Test
    void readsResponseCodesAndRequestBody() {
        OpenApiParseResult result = parser.parse(SPEC);

        assertThat(endpoint(result, "GET", "/accounts").getResponseCodes()).containsExactlyInAnyOrder("200", "401");
        assertThat(endpoint(result, "POST", "/payments").hasRequestBody()).isTrue();
        assertThat(endpoint(result, "GET", "/health").hasRequestBody()).isFalse();
    }

    @Test
    void readsTheShapeOfTheSuccessResponseAndFollowsReferences() {
        OpenApiParseResult result = parser.parse(SPEC);

        assertThat(endpoint(result, "GET", "/accounts").getResponseShape()).isEqualTo(ResponseShape.LIST);
        assertThat(endpoint(result, "GET", "/me").getResponseShape()).isEqualTo(ResponseShape.OBJECT);
        assertThat(endpoint(result, "GET", "/health").getResponseShape()).isEqualTo(ResponseShape.UNKNOWN);
    }

    @Test
    void followsReferencesInsideTheDocument() {
        OpenApiParseResult result = parser.parse("""
                openapi: 3.0.0
                info: {title: Refs, version: '1'}
                components:
                  parameters:
                    Limit: {name: limit, in: query, schema: {type: integer}}
                  responses:
                    UserResponse:
                      description: OK
                      content:
                        application/json:
                          schema: {type: object}
                paths:
                  /users:
                    get:
                      parameters:
                        - $ref: '#/components/parameters/Limit'
                      responses:
                        '200':
                          $ref: '#/components/responses/UserResponse'
                """);

        ApiEndpointInfo users = endpoint(result, "GET", "/users");
        assertThat(users.getQueryParameters()).containsExactly("limit");
        assertThat(users.getResponseShape()).isEqualTo(ResponseShape.OBJECT);
    }

    @Test
    void handlesOperationsWithoutSuccessResponses() {
        OpenApiParseResult result = parser.parse("""
                openapi: 3.0.0
                info: {title: Errors only, version: '1'}
                paths:
                  /legacy:
                    get:
                      responses:
                        '404': {description: Gone}
                """);

        assertThat(endpoint(result, "GET", "/legacy").getResponseShape()).isEqualTo(ResponseShape.UNKNOWN);
    }

    // Regression test for server-side request forgery: an uploaded file must not make the server call out.
    @Test
    void neverFetchesReferencesOutsideTheDocument() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer trap = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        trap.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        trap.start();
        String base = "http://127.0.0.1:" + trap.getAddress().getPort();
        try {
            OpenApiParseResult result = parser.parse("""
                    openapi: 3.0.0
                    info: {title: Remote refs, version: '1'}
                    paths:
                      /remote-path:
                        $ref: '%1$s/path-item'
                      /items:
                        get:
                          parameters:
                            - $ref: '%1$s/parameter'
                          responses:
                            '200':
                              description: OK
                              content:
                                application/json:
                                  schema:
                                    $ref: '%1$s/schema'
                            '404':
                              $ref: '%1$s/response'
                    """.formatted(base));

            assertThat(requests).hasValue(0);
            assertThat(endpoint(result, "GET", "/items").getResponseShape()).isEqualTo(ResponseShape.UNKNOWN);
        } finally {
            trap.stop(0);
        }
    }

    // A "billion laughs" document: under 1 KB of YAML aliases that expand to billions of nodes.
    @Test
    void rejectsYamlThatExpandsWithoutBoundsQuickly() {
        StringBuilder yaml = new StringBuilder("openapi: 3.0.0\ninfo: {title: bomb, version: '1'}\n");
        yaml.append("x-a0: &a0 [lol, lol, lol, lol, lol, lol, lol, lol, lol]\n");
        for (int level = 1; level <= 9; level++) {
            yaml.append("x-a").append(level).append(": &a").append(level).append(" [");
            yaml.append(String.join(", ", Collections.nCopies(9, "*a" + (level - 1))));
            yaml.append("]\n");
        }
        yaml.append("paths: {}\n");

        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThatThrownBy(() -> parser.parse(yaml.toString()))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("too complex"));
    }

    // Each operation becomes several findings, so an unbounded count could fill the database from one upload.
    @Test
    void rejectsMoreOperationsThanCanBeAnalysed() {
        StringBuilder yaml = new StringBuilder("openapi: 3.0.0\ninfo: {title: Many operations, version: '1'}\npaths:\n");
        for (int i = 0; i <= OpenApiParserService.MAX_OPERATIONS; i++) {
            yaml.append("  /items").append(i).append(":\n    get:\n      responses: {'200': {description: OK}}\n");
        }

        assertThatThrownBy(() -> parser.parse(yaml.toString()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most " + OpenApiParserService.MAX_OPERATIONS);
    }

    @Test
    void rejectsMoreServersThanCanBeAnalysed() {
        StringBuilder yaml = new StringBuilder("openapi: 3.0.0\ninfo: {title: Many servers, version: '1'}\nservers:\n");
        for (int i = 0; i <= OpenApiParserService.MAX_SERVERS; i++) {
            yaml.append("  - url: http://server").append(i).append(".example.com\n");
        }
        yaml.append("paths: {}\n");

        assertThatThrownBy(() -> parser.parse(yaml.toString()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most " + OpenApiParserService.MAX_SERVERS);
    }

    @Test
    void rejectsContentThatIsNotOpenApi() {
        assertThatThrownBy(() -> parser.parse("hello"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid OpenAPI");
    }
}
