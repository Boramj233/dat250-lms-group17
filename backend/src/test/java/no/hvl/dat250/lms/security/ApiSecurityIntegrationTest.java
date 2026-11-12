package no.hvl.dat250.lms.security;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;

import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The API filter chain (@Order(1), /api/**) as an OAuth 2.0 resource server for Keycloak
 * access tokens: 401 without a token or with one that is malformed, expired, signed with
 * an unknown key, for another issuer or audience, or without a verified email; 200 with a
 * valid one, no session, and authorization from the stored access level.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;

    private User saveUser(String email, Access access) {
        User user = new User(email, "Test", "User");
        user.setAccess(access);
        return users.save(user);
    }

    private static String bearer(User user) {
        return TestTokens.bearer(user);
    }

    private void expectRejected(String token) throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void requestWithoutTokenIsRejectedWith401AndNoLoginRedirect() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void malformedTokenIsRejectedWith401() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer error=\"invalid_token\"")))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void expiredTokenIsRejectedWith401() throws Exception {
        saveUser("student1@example.com", Access.STANDARD);
        Instant now = Instant.now();
        // Beyond the validator's 60 seconds of allowed clock skew.
        expectRejected(TestTokens.accessToken(claims -> claims
                .claim("email", "student1@example.com")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(120))));
    }

    @Test
    void tokenSignedWithAnUnknownKeyIsRejectedWith401() throws Exception {
        saveUser("student1@example.com", Access.STANDARD);
        expectRejected(TestTokens.accessTokenWithUnknownKey("student1@example.com"));
    }

    @Test
    void tokenFromAnotherIssuerIsRejectedWith401() throws Exception {
        saveUser("student1@example.com", Access.STANDARD);
        expectRejected(TestTokens.accessToken(claims -> claims
                .claim("email", "student1@example.com")
                .issuer("http://localhost:8081/realms/other")));
    }

    @Test
    void tokenForAnotherAudienceIsRejectedWith401() throws Exception {
        // A valid token of the realm, but not issued for this API (no lms-api audience).
        saveUser("student1@example.com", Access.STANDARD);
        expectRejected(TestTokens.accessToken(claims -> claims
                .claim("email", "student1@example.com")
                .audience(List.of("account"))));
    }

    @Test
    void tokenWithUnverifiedEmailIsRejectedWith401() throws Exception {
        saveUser("student1@example.com", Access.STANDARD);
        expectRejected(TestTokens.accessToken(claims -> claims
                .claim("email", "student1@example.com")
                .claim("email_verified", false)));
    }

    @Test
    void tokenForAnEmailWithoutLocalProfileIsRejectedWith401() throws Exception {
        expectRejected(TestTokens.accessToken("nobody@example.com"));
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
        MockHttpSession session = TestTokens.loginSession(student.getEmail());

        // The API chain never reads the session: only the Bearer token counts.
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
}
