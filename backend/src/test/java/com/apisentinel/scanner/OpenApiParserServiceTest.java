package com.apisentinel.scanner;

import com.apisentinel.exception.BadRequestException;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.model.OpenApiParseResult;
import com.apisentinel.scanner.model.ResponseShape;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void rejectsContentThatIsNotOpenApi() {
        assertThatThrownBy(() -> parser.parse("hello"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid OpenAPI");
    }
}
