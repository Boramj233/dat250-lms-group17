package no.hvl.dat250.lms.security;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The API filter chain (@Order(1), /api/**) with real tokens from {@link TokenService}:
 * 401 without/with an invalid/with an expired token, 200 with a valid one, no session,
 * and authorization from the stored access level.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityIntegrationTest {
    private static final Instant START = Instant.parse("2026-11-09T12:00:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;
    @MockitoBean private Clock clock;

    private Instant now;

    @BeforeEach
    void startClock() {
        now = START;
        when(clock.instant()).thenAnswer(call -> now);
    }

    private User saveUser(String email, Access access) {
        User user = new User(email, "Test", "User");
        user.setAccess(access);
        return users.save(user);
    }

    private String bearer(User user) {
        return TestTokens.bearer(tokens, user);
    }

    @Test
    void requestWithoutTokenIsRejectedWith401AndNoLoginRedirect() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void invalidTokenIsRejectedWith401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-token-we-issued"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer error=\"invalid_token\"")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void expiredTokenIsRejectedWith401() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        String header = bearer(student);

        now = START.plus(Duration.ofMinutes(15)).minusSeconds(1);
        mvc.perform(get("/api/users/me").header("Authorization", header))
                .andExpect(status().isOk());

        now = START.plus(Duration.ofMinutes(15));
        mvc.perform(get("/api/users/me").header("Authorization", header))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void validTokenGives200ForTheTokensUserAndCreatesNoSession() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        saveUser("student2@example.com", Access.STANDARD);
        var result = mvc.perform(get("/api/users/me").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(student.getUserId()))
                .andExpect(jsonPath("$.email").value("student1@example.com"))
                .andExpect(jsonPath("$.access").value("STANDARD"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        assertNull(result.getRequest().getSession(false), "the API chain must be stateless");
    }

    @Test
    void apiChainDoesNotAcceptTheLoginSession() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        MockHttpSession session = loginSession(student.getEmail());

        // The session is valid for the login chain ...
        mvc.perform(get("/token").session(session)).andExpect(status().isOk());
        // ... but the API chain never reads it: only the Bearer token counts.
        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void standardUserIsForbiddenOnCourseManagerEndpoint() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        mvc.perform(get("/api/users/search").param("search", "Test")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void courseManagerAndAdminMayUseCourseManagerEndpoint() throws Exception {
        // ADMIN is allowed through the role hierarchy (ADMIN > COURSE_MANAGER).
        for (User user : List.of(saveUser("teacher@example.com", Access.COURSE_MANAGER),
                                 saveUser("admin@example.com", Access.ADMIN))) {
            mvc.perform(get("/api/users/search").param("search", "Test")
                            .header("Authorization", bearer(user)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void accessChangeInDatabaseAppliesToTheNextRequestWithTheSameToken() throws Exception {
        User teacher = saveUser("teacher@example.com", Access.COURSE_MANAGER);
        String header = bearer(teacher);
        mvc.perform(get("/api/users/search").param("search", "Test").header("Authorization", header))
                .andExpect(status().isOk());
        teacher.setAccess(Access.STANDARD);
        mvc.perform(get("/api/users/search").param("search", "Test").header("Authorization", header))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenOfDeletedUserIsRejectedWith401() throws Exception {
        User teacher = saveUser("teacher@example.com", Access.COURSE_MANAGER);
        String header = bearer(teacher);
        users.delete(teacher);
        entities.flush();
        mvc.perform(get("/api/users/me").header("Authorization", header))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheUsersTokensButNotOtherUsersTokens() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        User other = saveUser("student2@example.com", Access.STANDARD);
        String studentToken = bearer(student);
        String otherToken = bearer(other);
        mvc.perform(get("/api/users/me").header("Authorization", studentToken))
                .andExpect(status().isOk());

        mvc.perform(post("/logout").session(loginSession(student.getEmail())))
                .andExpect(status().is3xxRedirection());

        // Same token, still within its lifetime: revoked by the logout.
        mvc.perform(get("/api/users/me").header("Authorization", studentToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/api/users/me").header("Authorization", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(other.getUserId()));
    }

    /** A session as the login chain stores it after a successful Keycloak login. */
    static MockHttpSession loginSession(String email) {
        Instant issued = Instant.now();
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://localhost:8081/realms/lms").subject("keycloak-id")
                .claim("email", email).claim("email_verified", true)
                .issuedAt(issued).expiresAt(issued.plusSeconds(300)).build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        var authentication = new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }
}
