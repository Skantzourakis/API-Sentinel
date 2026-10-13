package com.apisentinel.scanner.rules;

import com.apisentinel.entity.Finding;
import com.apisentinel.entity.FindingSeverity;
import com.apisentinel.scanner.model.ApiEndpointInfo;
import com.apisentinel.scanner.model.OpenApiParseResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRulesTest {

    // A public endpoint that documents a 400 response; tests change only what they check.
    private static ApiEndpointInfo endpoint(String method, String path) {
        return new ApiEndpointInfo(method, path, false, false, List.of(), Set.of("200", "400"));
    }

    private static ApiEndpointInfo secured(String method, String path) {
        return new ApiEndpointInfo(method, path, true, false, List.of(), Set.of("200", "400"));
    }

    private static ApiEndpointInfo withQuery(String method, String path, String... queryParameters) {
        return new ApiEndpointInfo(method, path, false, false, List.of(queryParameters), Set.of("200", "400"));
    }

    private static ApiEndpointInfo withResponses(String... codes) {
        return new ApiEndpointInfo("GET", "/items/{id}", false, false, List.of(), Set.of(codes));
    }

    @Nested
    class AdminWithoutAuth {

        private final SecurityRule rule = new AdminWithoutAuthRule();

        @Test
        void flagsPublicAdminEndpointAsCritical() {
            Optional<Finding> finding = rule.evaluate(null, endpoint("GET", "/admin/users"));

            assertThat(finding).isPresent();
            assertThat(finding.get().getRuleId()).isEqualTo("ADMIN_WITHOUT_AUTH");
            assertThat(finding.get().getSeverity()).isEqualTo(FindingSeverity.CRITICAL);
        }

        @Test
        void ignoresSecuredAdminEndpoint() {
            assertThat(rule.evaluate(null, secured("GET", "/admin/users"))).isEmpty();
        }

        @Test
        void ignoresNonAdminEndpoint() {
            assertThat(rule.evaluate(null, endpoint("GET", "/accounts"))).isEmpty();
        }
    }

    @Nested
    class UnauthenticatedWrite {

        private final SecurityRule rule = new UnauthenticatedWriteRule();

        @Test
        void flagsEveryPublicWriteMethod() {
            for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
                assertThat(rule.evaluate(null, endpoint(method, "/payments"))).as(method).isPresent();
            }
        }

        @Test
        void ignoresReadsAndSecuredWrites() {
            assertThat(rule.evaluate(null, endpoint("GET", "/payments"))).isEmpty();
            assertThat(rule.evaluate(null, secured("POST", "/payments"))).isEmpty();
        }
    }

    @Nested
    class PotentialBola {

        private final SecurityRule rule = new PotentialBolaRule();

        @Test
        void flagsPublicObjectEndpoint() {
            assertThat(rule.evaluate(null, endpoint("GET", "/accounts/{id}")))
                    .hasValueSatisfying(f -> assertThat(f.getSeverity()).isEqualTo(FindingSeverity.HIGH));
        }

        @Test
        void ignoresSecuredOrCollectionEndpoints() {
            assertThat(rule.evaluate(null, secured("GET", "/accounts/{id}"))).isEmpty();
            assertThat(rule.evaluate(null, endpoint("GET", "/accounts"))).isEmpty();
        }
    }

    @Nested
    class MissingRequestBody {

        private final SecurityRule rule = new MissingRequestBodyRule();

        @Test
        void flagsWriteWithoutBody() {
            assertThat(rule.evaluate(null, endpoint("POST", "/payments"))).isPresent();
        }

        @Test
        void ignoresWriteWithBodyAndReads() {
            ApiEndpointInfo withBody = new ApiEndpointInfo("POST", "/payments", false, true, List.of(), Set.of("400"));
            assertThat(rule.evaluate(null, withBody)).isEmpty();
            assertThat(rule.evaluate(null, endpoint("GET", "/payments"))).isEmpty();
        }
    }

    @Nested
    class SensitiveDataInQuery {

        private final SecurityRule rule = new SensitiveDataInQueryRule();

        @Test
        void flagsSensitiveNamesRegardlessOfCaseAndSeparators() {
            for (String name : List.of("password", "api_key", "X-Api-Key", "accessToken", "client_secret")) {
                assertThat(rule.evaluate(null, withQuery("GET", "/login", name))).as(name).isPresent();
            }
        }

        @Test
        void listsTheSensitiveParametersInTheDescription() {
            Optional<Finding> finding = rule.evaluate(null, withQuery("GET", "/login", "page", "password"));

            assertThat(finding).hasValueSatisfying(f -> {
                assertThat(f.getDescription()).contains("password");
                assertThat(f.getDescription()).doesNotContain("page");
            });
        }

        @Test
        void ignoresHarmlessParameters() {
            assertThat(rule.evaluate(null, withQuery("GET", "/accounts", "page", "size", "sort"))).isEmpty();
        }
    }

    @Nested
    class MissingPagination {

        private final SecurityRule rule = new MissingPaginationRule();

        @Test
        void flagsListWithoutPaginationParameters() {
            assertThat(rule.evaluate(null, endpoint("GET", "/accounts"))).isPresent();
        }

        @Test
        void acceptsCommonPaginationStyles() {
            assertThat(rule.evaluate(null, withQuery("GET", "/accounts", "limit"))).isEmpty();
            assertThat(rule.evaluate(null, withQuery("GET", "/accounts", "page_size"))).isEmpty();
            assertThat(rule.evaluate(null, withQuery("GET", "/accounts", "cursor"))).isEmpty();
        }

        @Test
        void ignoresSingleItemAndWriteEndpoints() {
            assertThat(rule.evaluate(null, endpoint("GET", "/accounts/{id}"))).isEmpty();
            assertThat(rule.evaluate(null, endpoint("POST", "/accounts"))).isEmpty();
        }
    }

    @Nested
    class MissingErrorResponses {

        private final SecurityRule rule = new MissingErrorResponsesRule();

        @Test
        void flagsOperationWithOnlySuccessResponses() {
            assertThat(rule.evaluate(null, withResponses("200", "201")))
                    .hasValueSatisfying(f -> assertThat(f.getSeverity()).isEqualTo(FindingSeverity.LOW));
        }

        @Test
        void acceptsAny4xxOrDefaultResponse() {
            assertThat(rule.evaluate(null, withResponses("200", "404"))).isEmpty();
            assertThat(rule.evaluate(null, withResponses("200", "default"))).isEmpty();
        }
    }

    @Nested
    class InsecureServerUrl {

        private final ApiSpecRule rule = new InsecureServerUrlRule();

        private List<Finding> evaluate(String... serverUrls) {
            return rule.evaluate(null, new OpenApiParseResult(List.of(), List.of(serverUrls)));
        }

        @Test
        void flagsPlainHttpServer() {
            List<Finding> findings = evaluate("http://api.example.com");

            assertThat(findings).hasSize(1);
            assertThat(findings.get(0).getPath()).isEqualTo("http://api.example.com");
        }

        @Test
        void ignoresHttpsAndLocalServers() {
            assertThat(evaluate("https://api.example.com", "http://localhost:8080", "/")).isEmpty();
        }
    }
}
