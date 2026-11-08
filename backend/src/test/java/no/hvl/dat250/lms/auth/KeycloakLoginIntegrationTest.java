package no.hvl.dat250.lms.auth;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt in with LMS_KEYCLOAK_TEST=true and the client/demo secrets in the environment. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "LMS_KEYCLOAK_TEST", matches = "true")
class KeycloakLoginIntegrationTest {
    @Autowired private MockMvc mvc;

    @DynamicPropertySource
    static void realClientSecret(DynamicPropertyRegistry properties) {
        properties.add("spring.security.oauth2.client.registration.keycloak.client-secret",
                () -> System.getenv("KEYCLOAK_CLIENT_SECRET"));
    }

    private record LoginAttempt(MockHttpSession session, URI destination) {}

    private LoginAttempt attemptLogin(String username, String password) throws Exception {
        var start = mvc.perform(get("http://localhost:8080/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) start.getRequest().getSession(false);
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
        var page = client.send(HttpRequest.newBuilder(URI.create(start.getResponse().getRedirectedUrl()))
                .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        var action = Pattern.compile("<form[^>]*action=\"([^\"]+)\"").matcher(page.body());
        assertTrue(action.find(), "Keycloak must present a login form");
        String cookies = page.headers().allValues("Set-Cookie").stream()
                .map(value -> value.split(";", 2)[0]).collect(Collectors.joining("; "));
        String body = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        var response = client.send(HttpRequest.newBuilder(URI.create(action.group(1).replace("&amp;", "&")))
                .timeout(Duration.ofSeconds(20)).header("Cookie", cookies)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) return new LoginAttempt(session, null);
        assertEquals(302, response.statusCode(), "Keycloak must return a callback after successful login");
        return new LoginAttempt(session, URI.create(response.headers().firstValue("Location").orElseThrow()));
    }

    @Test
    void realKeycloakLoginIssuesApplicationTokenAndReturnsMatchingProfile() throws Exception {
        for (String username : new String[] {"student1", "student2"}) {
            var login = attemptLogin(username, System.getenv("KEYCLOAK_DEMO_PASSWORD"));
            assertNotNull(login.destination(), "Correct credentials must produce a callback");
            var params = UriComponentsBuilder.fromUri(login.destination()).build().getQueryParams();
            var callback = mvc.perform(get("http://localhost:8080/login/oauth2/code/keycloak")
                            .session(login.session()).param("code", URLDecoder.decode(params.getFirst("code"), StandardCharsets.UTF_8))
                            .param("state", URLDecoder.decode(params.getFirst("state"), StandardCharsets.UTF_8)))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                    .andReturn();
            var session = (MockHttpSession) callback.getRequest().getSession(false);
            String json = mvc.perform(get("/token").session(session)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String token = JsonPath.read(json, "$.accessToken");
            mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(username + "@example.com"))
                    .andExpect(jsonPath("$.access").value("STANDARD"));
            mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void wrongPasswordDoesNotEstablishAnApplicationSession() throws Exception {
        var login = attemptLogin("student1", "deliberately-wrong-password");
        assertNull(login.destination());
        mvc.perform(get("/token").session(login.session())).andExpect(status().isUnauthorized());
    }
}
