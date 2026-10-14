package com.apisentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs with low limits. Counters are kept for the whole test class, so every test uses its own addresses and emails.
@SpringBootTest(properties = {
        "app.rate-limit.logins-per-address-per-minute=3",
        "app.rate-limit.logins-per-email-per-15-minutes=5",
        "app.rate-limit.registrations-per-address-per-hour=2",
        "app.rate-limit.uploads-per-user-per-minute=2"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    private static final String PASSWORD = "supersecret1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private String uniqueEmail() {
        return "limits-" + UUID.randomUUID() + "@example.com";
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private ResultActions register(String email, String address) throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(from(address))
                .contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)));
    }

    private ResultActions login(String email, String password, String address) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(from(address))
                .contentType(MediaType.APPLICATION_JSON).content(credentials(email, password)));
    }

    // Guessing one account's password from many machines is stopped by the per-email limit.
    @Test
    void anAccountIsBlockedAfterFiveFailedLoginsFromAnyAddresses() throws Exception {
        String email = uniqueEmail();
        register(email, "10.0.1.1").andExpect(status().isCreated());

        for (int attempt = 1; attempt <= 5; attempt++) {
            login(email, "wrong-password", "10.0.1." + (attempt + 1)).andExpect(status().isUnauthorized());
        }

        // Even the right password is refused until the window ends.
        login(email, PASSWORD, "10.0.1.10")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.details[0]").value("Too many requests. Try again in 15 minutes."));
    }

    @Test
    void aSuccessfulLoginClearsTheAccountsFailures() throws Exception {
        String email = uniqueEmail();
        register(email, "10.0.2.1").andExpect(status().isCreated());

        for (int attempt = 1; attempt <= 4; attempt++) {
            login(email, "wrong-password", "10.0.2." + (attempt + 1)).andExpect(status().isUnauthorized());
        }
        login(email, PASSWORD, "10.0.2.10").andExpect(status().isOk());
        login(email, "wrong-password", "10.0.2.11").andExpect(status().isUnauthorized());
        login(email, PASSWORD, "10.0.2.12").andExpect(status().isOk());
    }

    // Trying many accounts from one machine (credential stuffing) is stopped by the per-address limit.
    @Test
    void oneAddressIsLimitedAcrossAllEmails() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            login(uniqueEmail(), PASSWORD, "10.0.3.1").andExpect(status().isUnauthorized());
        }

        login(uniqueEmail(), PASSWORD, "10.0.3.1").andExpect(status().isTooManyRequests());
        login(uniqueEmail(), PASSWORD, "10.0.3.2").andExpect(status().isUnauthorized());
    }

    @Test
    void registrationsAreLimitedPerAddress() throws Exception {
        register(uniqueEmail(), "10.0.4.1").andExpect(status().isCreated());
        register(uniqueEmail(), "10.0.4.1").andExpect(status().isCreated());

        register(uniqueEmail(), "10.0.4.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void uploadsAreLimitedPerUser() throws Exception {
        String email = uniqueEmail();
        register(email, "10.0.5.1").andExpect(status().isCreated());
        String body = login(email, PASSWORD, "10.0.5.1").andReturn().getResponse().getContentAsString();
        String token = "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
        String project = mockMvc.perform(post("/api/projects").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Limits\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(project).get("id").asLong();
        MockMultipartFile file = new MockMultipartFile("file", "broken.yaml", "application/yaml", "hello".getBytes());

        for (int upload = 1; upload <= 2; upload++) {
            mockMvc.perform(multipart("/api/projects/{id}/scans/upload", projectId).file(file).header("Authorization", token))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(multipart("/api/projects/{id}/scans/upload", projectId).file(file).header("Authorization", token))
                .andExpect(status().isTooManyRequests());
    }
}
