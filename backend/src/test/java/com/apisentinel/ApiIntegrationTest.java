package com.apisentinel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs the whole application against the in-memory H2 database and calls it over HTTP.
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String PASSWORD = "supersecret1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Tests share one database, so every test registers its own users.
    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + json(login).get("accessToken").asText();
    }

    private long createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long uploadSample(String token, long projectId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample-openapi.yaml", "application/yaml", Files.readAllBytes(Path.of("sample-openapi.yaml")));
        MvcResult result = mockMvc.perform(multipart("/api/projects/{id}/scans/upload", projectId)
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsDuplicatesAndInvalidInput() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        // Emails are compared case-insensitively.
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials("not-an-email", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void currentUserEndpointReturnsTheSignedInAccount() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(startsWith("user-")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(email, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scanningTheSampleProducesFindingsSummaryAndPages() throws Exception {
        String token = registerAndLogin();
        long projectId = createProject(token, "Sample");
        long scanId = uploadSample(token, projectId);

        // The scan list carries per-severity counts so clients can draw each scan's severity bar.
        mockMvc.perform(get("/api/projects/{id}/scans", projectId).header("Authorization", token))
                .andExpect(jsonPath("$[0].findingsBySeverity.HIGH").value(6))
                .andExpect(jsonPath("$[0].findingsBySeverity.LOW").value(4));

        // The project list summarises each project's latest scan.
        mockMvc.perform(get("/api/projects").header("Authorization", token))
                .andExpect(jsonPath("$[0].scanCount").value(1))
                .andExpect(jsonPath("$[0].latestScan.id").value(scanId))
                .andExpect(jsonPath("$[0].latestScan.findingsBySeverity.CRITICAL").value(1));

        mockMvc.perform(get("/api/scans/{id}/summary", scanId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFindings").value(14))
                .andExpect(jsonPath("$.findingsBySeverity.CRITICAL").value(1))
                .andExpect(jsonPath("$.riskScore").value(100));

        // Most severe first, five per page.
        mockMvc.perform(get("/api/scans/{id}/findings", scanId).param("size", "5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.totalElements").value(14))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/scans/{id}/findings", scanId).param("severity", "LOW").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(4));

        mockMvc.perform(get("/api/scans/{id}/findings", scanId).param("size", "500").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidUploadIsRecordedAsFailedScan() throws Exception {
        String token = registerAndLogin();
        long projectId = createProject(token, "Broken upload");
        MockMultipartFile file = new MockMultipartFile("file", "broken.yaml", "application/yaml", "hello".getBytes());

        mockMvc.perform(multipart("/api/projects/{id}/scans/upload", projectId).file(file).header("Authorization", token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/projects/{id}/scans", projectId).header("Authorization", token))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    void comparingAScanWithItselfShowsNoChanges() throws Exception {
        String token = registerAndLogin();
        long projectId = createProject(token, "Compare");
        long firstScan = uploadSample(token, projectId);
        long secondScan = uploadSample(token, projectId);

        mockMvc.perform(get("/api/scans/{id}/comparison", secondScan).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseScanId").value(firstScan))
                .andExpect(jsonPath("$.newFindings.length()").value(0))
                .andExpect(jsonPath("$.resolvedFindings.length()").value(0))
                .andExpect(jsonPath("$.unchangedFindings").value(14));

        mockMvc.perform(get("/api/scans/{id}/comparison", firstScan).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeOrChangeEachOthersData() throws Exception {
        String alice = registerAndLogin();
        String bob = registerAndLogin();
        long projectId = createProject(alice, "Alice API");
        long scanId = uploadSample(alice, projectId);

        mockMvc.perform(get("/api/projects").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Another user's resources look exactly like missing ones.
        for (String url : new String[]{
                "/api/projects/" + projectId,
                "/api/projects/" + projectId + "/scans",
                "/api/scans/" + scanId,
                "/api/scans/" + scanId + "/findings",
                "/api/scans/" + scanId + "/summary",
                "/api/scans/" + scanId + "/comparison"}) {
            int status = mockMvc.perform(get(url).header("Authorization", bob)).andReturn().getResponse().getStatus();
            assertThat(status).as(url).isEqualTo(404);
        }

        mockMvc.perform(delete("/api/projects/{id}", projectId).header("Authorization", bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{id}", projectId).header("Authorization", alice))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAProjectRemovesItsScans() throws Exception {
        String token = registerAndLogin();
        long projectId = createProject(token, "To delete");
        long scanId = uploadSample(token, projectId);

        mockMvc.perform(delete("/api/projects/{id}", projectId).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/scans/{id}", scanId).header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
