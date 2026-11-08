package no.hvl.dat250.lms.auth;

import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private AuthService auth;
    @Autowired private ApiTokenService tokens;
    @Autowired private UserRepository users;

    private MockHttpSession loginSession() {
        var identity = AuthServiceTest.identity("sara@example.com", true);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(new OAuth2AuthenticationToken(identity, identity.getAuthorities(), "keycloak")));
        return session;
    }

    @Test
    void oidcProfileIsCreatedOnceAndExistingAccessIsPreserved() {
        var identity = AuthServiceTest.identity("sara@example.com", true);
        User first = auth.provisionUser(identity);
        assertEquals(Access.STANDARD, first.getAccess());
        first.setAccess(Access.COURSE_MANAGER);
        assertEquals(first.getUserId(), auth.provisionUser(identity).getUserId());
        assertEquals(Access.COURSE_MANAGER, users.findById(first.getUserId()).orElseThrow().getAccess());
        assertEquals(1, users.count());
    }

    @Test
    void sessionCanIssueTokenButCannotAuthenticateTheApi() throws Exception {
        auth.provisionUser(AuthServiceTest.identity("sara@example.com", true));
        var session = loginSession();
        mvc.perform(get("/token").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresAt").isString());
        mvc.perform(get("/api/users/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenEndpointRequiresAnOidcSession() throws Exception {
        mvc.perform(get("/token")).andExpect(status().isUnauthorized());
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        mvc.perform(get("/token").header("Authorization", "Bearer " + tokens.issue(user).accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenReturnsOnlyItsOwnProfileAndCreatesNoSession() throws Exception {
        User sara = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        User other = users.save(new User("other@example.com", "Ola", "Nordmann"));
        for (User user : new User[] {sara, other}) {
            var result = mvc.perform(get("/api/users/me")
                            .header("Authorization", "Bearer " + tokens.issue(user).accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(user.getUserId()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andReturn();
            assertNull(result.getRequest().getSession(false));
        }
    }

    @Test
    void badAndRevokedTokensReturn401WithoutLoginRedirect() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        String token = tokens.issue(user).accessToken();
        tokens.revoke(token);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginStartsWithKeycloakRedirect() throws Exception {
        mvc.perform(get("/oauth2/authorization/keycloak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/realms/lms/protocol/openid-connect/auth")));
    }
}
