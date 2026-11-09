package no.hvl.dat250.lms.security;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The login filter chain (@Order(2)): Keycloak login, GET /token, static files, logout. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginSecurityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;

    @Test
    void loginStartsTheAuthorizationCodeFlowAtThePublicKeycloakUrl() throws Exception {
        mvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        startsWith("http://localhost:8081/realms/lms/protocol/openid-connect/auth?"),
                        containsString("response_type=code"),
                        containsString("client_id=spring"),
                        containsString("scope=openid%20profile%20email"),
                        containsString("redirect_uri=http://localhost/login/oauth2/code/keycloak"))));
    }

    @Test
    void tokenWithoutSessionIs401WithoutRedirect() throws Exception {
        mvc.perform(get("/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void tokenWithSessionReturnsAnUncachedTokenThatWorksOnTheApi() throws Exception {
        User user = users.save(new User("student1@example.com", "Demo", "Student One"));
        String body = mvc.perform(get("/token").with(oidcLogin().idToken(token -> token
                        .claim("email", user.getEmail()).claim("email_verified", true))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String token = JsonMapper.builder().build().readTree(body).get("accessToken").asString();
        // 32 random bytes, base64url without padding.
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"), token);

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.firstName").value("Demo"));
    }

    @Test
    void tokenIsRefusedForUnverifiedEmail() throws Exception {
        users.save(new User("student1@example.com", "Demo", "Student One"));
        mvc.perform(get("/token").with(oidcLogin().idToken(token -> token
                        .claim("email", "student1@example.com").claim("email_verified", false))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staticFrontendFilesAreOpenWithoutToken() throws Exception {
        // The backend has no frontend build of its own (nginx serves it in Docker), so the
        // answer is 404; what matters is that security lets the request through.
        for (String path : new String[] {"/", "/index.html", "/assets/index.js", "/favicon.svg"}) {
            mvc.perform(get(path)).andExpect(status().is(not(anyOf(is(401), is(403)))));
        }
    }

    @Test
    void everythingElseIsClosedIncludingTheH2Console() throws Exception {
        for (String path : new String[] {"/h2-console", "/h2-console/login.do", "/actuator", "/login"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void logoutEndsTheSessionAndTheKeycloakSession() throws Exception {
        var session = ApiSecurityIntegrationTest.loginSession("student1@example.com");
        mvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        startsWith("http://localhost:8081/realms/lms/protocol/openid-connect/logout?"),
                        containsString("client_id=spring"),
                        containsString("post_logout_redirect_uri=http://localhost/"),
                        containsString("id_token_hint=id-token"))));
        assertTrue(session.isInvalid());
    }
}
