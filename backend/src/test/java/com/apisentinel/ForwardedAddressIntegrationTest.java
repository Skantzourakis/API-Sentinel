package com.apisentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The setting used behind nginx (see docker-compose.yml): X-Forwarded-For from a trusted proxy names the client.
// Runs on a real server, because Tomcat reads the header before the request reaches Spring MVC.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.forward-headers-strategy=native",
        "app.rate-limit.logins-per-address-per-minute=2"
})
class ForwardedAddressIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // The test's requests come from 127.0.0.1, which stands in for the nginx container here.
    private HttpStatusCode loginForClient(String clientAddress) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Forwarded-For", clientAddress);
        String body = "{\"email\":\"forwarded-" + UUID.randomUUID() + "@example.com\",\"password\":\"supersecret1\"}";
        return restTemplate.postForEntity("/api/auth/login", new HttpEntity<>(body, headers), String.class).getStatusCode();
    }

    @Test
    void eachClientBehindTheProxyHasItsOwnLimit() {
        assertThat(loginForClient("203.0.113.1")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginForClient("203.0.113.1")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginForClient("203.0.113.1")).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(loginForClient("203.0.113.2")).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
