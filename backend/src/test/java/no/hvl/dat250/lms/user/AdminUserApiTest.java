package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;
    private User admin;
    private User user;
    private User other;

    @BeforeEach
    void createAccounts() {
        admin = new User("admin-hash", "System", "Administrator");
        admin.setAccess(Access.ADMIN);
        admin.changePassword("permanent-admin-hash");
        users.save(admin);
        user = users.save(new User("private-user-hash", "Sara", "Nordmann"));
        other = users.save(new User("private-other-hash", "Ola", "Hansen"));
    }

    private MockHttpServletRequestBuilder as(User actor, MockHttpServletRequestBuilder builder) {
        MockHttpSession session = new MockHttpSession();
        if (actor != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                            actor, null, actor.getAuthorities())));
        }
        var token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "admin-test-token");
        var request = new MockHttpServletRequest();
        request.setSession(session);
        var response = new MockHttpServletResponse();
        new HttpSessionCsrfTokenRepository().saveToken(token, request, response);
        new XorCsrfTokenRequestAttributeHandler().handle(request, response, () -> token);
        CsrfToken exposed = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return builder.session(session).header(token.getHeaderName(), exposed.getToken())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
    }

    @Test
    void listsStablePagesWithoutCredentials() throws Exception {
        mvc.perform(as(admin, get("/api/admin/users").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(2)))
                .andExpect(jsonPath("$.users[0].userId").value(admin.getUserId()))
                .andExpect(jsonPath("$.users[1].userId").value(user.getUserId()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(content().string(not(containsString("hash"))))
                .andExpect(content().string(not(containsString("passwordHash"))));
        mvc.perform(as(admin, get("/api/admin/users").param("size", "2").param("page", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0].userId").value(other.getUserId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sArA", " NORDMANN "})
    void searchesNamesIgnoringCase(String search) throws Exception {
        mvc.perform(as(admin, get("/api/admin/users").param("search", search)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.users[0].userId").value(user.getUserId()));
    }

    @Test
    void searchesGeneratedEmailAndNumericId() throws Exception {
        mvc.perform(as(admin, get("/api/admin/users").param("search", user.getEmail().toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.users[0].userId").value(user.getUserId()));
        mvc.perform(as(admin, get("/api/admin/users").param("search", user.getUserId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[?(@.userId == " + user.getUserId() + ")]").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-user", "%", "_", "999999999999999999999999999999999"})
    void unmatchedSearchReturnsEmptyPage(String search) throws Exception {
        mvc.perform(as(admin, get("/api/admin/users").param("search", search)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void retrievesUserByIdWithoutCredentials() throws Exception {
        mvc.perform(as(admin, get("/api/admin/users/{id}", user.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(6)))
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(content().string(not(containsString("private-user-hash"))));
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void administratorCanAssignEachAccessLevelWithoutChangingOtherFields(Access access) throws Exception {
        mvc.perform(as(admin, patch("/api/admin/users/{id}/access", user.getUserId()))
                        .content("{\"access\":\"" + access + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value(access.name()))
                .andExpect(content().string(not(containsString("private-user-hash"))));
        entities.flush();
        entities.clear();
        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        assertEquals(access, stored.getAccess());
        assertEquals("Sara", stored.getFirstName());
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals("private-user-hash", stored.getPassword());
        assertTrue(stored.isMustChangePassword());
    }

    @Test
    void administratorCannotRemoveOwnAccess() throws Exception {
        mvc.perform(as(admin, patch("/api/admin/users/{id}/access", admin.getUserId()))
                        .content("{\"access\":\"STANDARD\"}"))
                .andExpect(status().isForbidden());
        entities.flush();
        entities.clear();
        assertEquals(Access.ADMIN, users.findByUserId(admin.getUserId()).orElseThrow().getAccess());
    }

    @ParameterizedTest
    @EnumSource(value = Access.class, names = {"STANDARD", "COURSE_MANAGER"})
    void nonAdministratorsCannotListReadOrChangeUsers(Access access) throws Exception {
        user.setAccess(access);
        mvc.perform(as(user, get("/api/admin/users"))).andExpect(status().isForbidden());
        mvc.perform(as(user, get("/api/admin/users/{id}", other.getUserId())))
                .andExpect(status().isForbidden());
        mvc.perform(as(user, patch("/api/admin/users/{id}/access", other.getUserId()))
                        .content("{\"access\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertEquals(Access.STANDARD, users.findByUserId(other.getUserId()).orElseThrow().getAccess());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(as(null, get("/api/admin/users"))).andExpect(status().isUnauthorized());
        mvc.perform(as(null, get("/api/admin/users/{id}", user.getUserId())))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(null, patch("/api/admin/users/{id}/access", user.getUserId()))
                        .content("{\"access\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void outdatedAdminSessionCannotRetainPrivileges() throws Exception {
        entities.flush();
        entities.detach(admin);
        User storedAdmin = users.findByUserId(admin.getUserId()).orElseThrow();
        storedAdmin.setAccess(Access.STANDARD);
        assertEquals(Access.ADMIN, admin.getAccess());
        mvc.perform(as(admin, get("/api/admin/users"))).andExpect(status().isForbidden());
        mvc.perform(as(admin, patch("/api/admin/users/{id}/access", user.getUserId()))
                        .content("{\"access\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertEquals(Access.STANDARD, user.getAccess());
    }

    @Test
    void unknownTargetsReturnNotFound() throws Exception {
        mvc.perform(as(admin, get("/api/admin/users/{id}", Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
        mvc.perform(as(admin, patch("/api/admin/users/{id}/access", Long.MAX_VALUE))
                        .content("{\"access\":\"STANDARD\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"access\":null}", "{\"access\":\"INVALID\"}"})
    void invalidAccessRequestsDoNotChangeUser(String body) throws Exception {
        mvc.perform(as(admin, patch("/api/admin/users/{id}/access", user.getUserId())).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(Access.STANDARD, users.findByUserId(user.getUserId()).orElseThrow().getAccess());
    }

    @Test
    void invalidPaginationAndOversizedSearchAreRejected() throws Exception {
        for (String size : new String[]{"0", "101", "-1"}) {
            mvc.perform(as(admin, get("/api/admin/users").param("size", size)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(as(admin, get("/api/admin/users").param("page", "-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(as(admin, get("/api/admin/users").param("search", "a".repeat(256))))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Access.class, names = {"COURSE_MANAGER", "ADMIN"})
    void managersAndAdministratorsCanSearchOnlyRestrictedIdentityFields(Access access) throws Exception {
        other.setAccess(access);
        mvc.perform(as(other, get("/api/users/search").param("search", "sArA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.users[0]", aMapWithSize(4)))
                .andExpect(jsonPath("$.users[0].userId").value(user.getUserId()))
                .andExpect(jsonPath("$.users[0].firstName").value("Sara"))
                .andExpect(jsonPath("$.users[0].lastName").value("Nordmann"))
                .andExpect(jsonPath("$.users[0].email").value(user.getEmail()))
                .andExpect(content().string(not(containsString("mustChangePassword"))))
                .andExpect(content().string(not(containsString("access"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("hash"))));
        assertEquals(3, users.count());
        assertEquals(Access.STANDARD, user.getAccess());
    }

    @Test
    void courseManagerCanSearchByLastNameEmailAndUserId() throws Exception {
        other.setAccess(Access.COURSE_MANAGER);
        for (String search : new String[]{" NORDMANN ", user.getEmail().toUpperCase()}) {
            mvc.perform(as(other, get("/api/users/search").param("search", search)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.users[0].userId").value(user.getUserId()));
        }
        mvc.perform(as(other, get("/api/users/search").param("search", user.getUserId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[?(@.userId == " + user.getUserId() + ")]").isNotEmpty());
    }

    @Test
    void restrictedSearchIsPaginatedAndCanReturnNoMatches() throws Exception {
        other.setAccess(Access.COURSE_MANAGER);
        mvc.perform(as(other, get("/api/users/search").param("search", "@epost.com")
                        .param("page", "1").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0].userId").value(other.getUserId()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(as(other, get("/api/users/search").param("search", "unknown-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(0)));
    }

    @Test
    void standardAndAnonymousUsersCannotSearchOtherAccounts() throws Exception {
        mvc.perform(as(user, get("/api/users/search").param("search", "Sara")))
                .andExpect(status().isForbidden());
        mvc.perform(as(null, get("/api/users/search").param("search", "Sara")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void outdatedCourseManagerSessionCannotSearchAccounts() throws Exception {
        other.setAccess(Access.COURSE_MANAGER);
        entities.flush();
        entities.detach(other);
        users.findByUserId(other.getUserId()).orElseThrow().setAccess(Access.STANDARD);
        assertEquals(Access.COURSE_MANAGER, other.getAccess());
        mvc.perform(as(other, get("/api/users/search").param("search", "Sara")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedCourseManagerCannotSearchAccounts() throws Exception {
        other.setAccess(Access.COURSE_MANAGER);
        users.delete(other);
        mvc.perform(as(other, get("/api/users/search").param("search", "Sara")))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictedSearchRequiresSearchTextAndValidPageParameters() throws Exception {
        other.setAccess(Access.COURSE_MANAGER);
        mvc.perform(as(other, get("/api/users/search"))).andExpect(status().isBadRequest());
        for (String search : new String[]{"", "   ", "a".repeat(256)}) {
            mvc.perform(as(other, get("/api/users/search").param("search", search)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(as(other, get("/api/users/search").param("search", "Sara").param("page", "-1")))
                .andExpect(status().isBadRequest());
        for (String size : new String[]{"0", "101"}) {
            mvc.perform(as(other, get("/api/users/search").param("search", "Sara").param("size", size)))
                    .andExpect(status().isBadRequest());
        }
    }
}
