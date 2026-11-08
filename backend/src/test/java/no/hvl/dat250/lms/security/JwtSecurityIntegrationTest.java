package no.hvl.dat250.lms.security;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static no.hvl.dat250.lms.security.TestTokens.bearer;
import static no.hvl.dat250.lms.security.TestTokens.bearerUnverified;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The API as a JWT resource server: 401 / 200 / 403 and linking tokens to local users. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtSecurityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;
    @MockitoBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void acceptTestTokens() {
        TestTokens.install(jwtDecoder);
    }

    private User saveUser(String email, Access access) {
        User user = new User(email, "Test", "User");
        user.setAccess(access);
        return users.save(user);
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
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenForStandardUserGives200AndCreatesNoSession() throws Exception {
        User student = saveUser("student1@example.com", Access.STANDARD);
        var result = mvc.perform(get("/api/users/me").header("Authorization", bearer(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(student.getUserId()))
                .andExpect(jsonPath("$.email").value("student1@example.com"))
                .andExpect(jsonPath("$.access").value("STANDARD"))
                .andReturn();
        assertNull(result.getRequest().getSession(false), "the API must be stateless");
    }

    @Test
    void standardUserIsForbiddenOnCourseManagerEndpoint() throws Exception {
        saveUser("student1@example.com", Access.STANDARD);
        mvc.perform(get("/api/users/search").param("search", "Test")
                        .header("Authorization", bearer("student1@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void courseManagerAndAdminMayUseCourseManagerEndpoint() throws Exception {
        saveUser("teacher@example.com", Access.COURSE_MANAGER);
        saveUser("admin@example.com", Access.ADMIN);
        // ADMIN is allowed through the role hierarchy (ADMIN > COURSE_MANAGER).
        for (String email : new String[] {"teacher@example.com", "admin@example.com"}) {
            mvc.perform(get("/api/users/search").param("search", "Test")
                            .header("Authorization", bearer(email)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void firstRequestCreatesStandardProfileOnceAndStoredAccessIsKept() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer("new@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.access").value("STANDARD"));
        User created = users.findByEmail("new@example.com").orElseThrow();
        created.setAccess(Access.COURSE_MANAGER);

        mvc.perform(get("/api/users/me").header("Authorization", bearer("new@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(created.getUserId()))
                .andExpect(jsonPath("$.access").value("COURSE_MANAGER"));
        assertEquals(1, users.count());
    }

    @Test
    void accessChangeInDatabaseAppliesToTheNextRequest() throws Exception {
        User teacher = saveUser("teacher@example.com", Access.COURSE_MANAGER);
        mvc.perform(get("/api/users/search").param("search", "Test")
                        .header("Authorization", bearer(teacher.getEmail())))
                .andExpect(status().isOk());
        teacher.setAccess(Access.STANDARD);
        // Same (still valid) token, but the role is read from the database on every request.
        mvc.perform(get("/api/users/search").param("search", "Test")
                        .header("Authorization", bearer(teacher.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unverifiedEmailIsRejectedAndNoProfileIsCreated() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearerUnverified("admin@example.com")))
                .andExpect(status().isUnauthorized());
        assertTrue(users.findByEmail("admin@example.com").isEmpty());
    }

    @Test
    void deletedLocalProfileIsRecreatedAsStandardOnNextRequest() throws Exception {
        // Identity lives in Keycloak; to block someone, disable them there. Deleting the
        // local profile only resets it: privileges are not restored.
        User teacher = saveUser("teacher@example.com", Access.COURSE_MANAGER);
        Long oldId = teacher.getUserId();
        users.delete(teacher);
        entities.flush();
        mvc.perform(get("/api/users/search").param("search", "Test")
                        .header("Authorization", bearer("teacher@example.com")))
                .andExpect(status().isForbidden());
        User recreated = users.findByEmail("teacher@example.com").orElseThrow();
        assertNotEquals(oldId, recreated.getUserId());
        assertEquals(Access.STANDARD, recreated.getAccess());
    }
}
