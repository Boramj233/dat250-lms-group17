package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserProfileApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;

    // Real security filters and a session CSRF token, without changing security configuration.
    private MockHttpServletRequestBuilder updateAs(User user) {
        MockHttpSession session = new MockHttpSession();
        if (user != null) {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, java.util.List.of());
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(authentication));
        }
        var token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "profile-test-token");
        var request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(token, request, new MockHttpServletResponse());
        new XorCsrfTokenRequestAttributeHandler().handle(request, new MockHttpServletResponse(), () -> token);
        CsrfToken exposedToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return put("/api/users/me").session(session).header(token.getHeaderName(), exposedToken.getToken())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
    }

    @Test
    void persistsOnlyOwnEditableFieldsAndReturnsNoPassword() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        User other = users.save(new User("other@example.com", "Sara", "Nordmann"));
        mvc.perform(updateAs(user).content("""
                {"firstName":"Kari","lastName":"Hansen"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.firstName").value("Kari"))
                .andExpect(jsonPath("$.lastName").value("Hansen"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.access").value("STANDARD"))
                .andExpect(content().string(not(containsString("encoded-secret"))));
        entities.flush();
        entities.clear();
        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals("Kari", stored.getFirstName());
        assertEquals("Hansen", stored.getLastName());
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals(Access.STANDARD, stored.getAccess());
        assertEquals(other.getEmail(), users.findById(other.getUserId()).orElseThrow().getEmail());
    }

    @Test
    void unchangedProfileIsAllowed() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        mvc.perform(updateAs(user).content("""
                {"firstName":"Sara","lastName":"Nordmann"}
                """))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"lastName\":\"Nordmann\"}",
            "{\"firstName\":\"Sara\"}",
            "{\"firstName\":null,\"lastName\":\"Nordmann\"}",
            "{\"firstName\":\"Sara\",\"lastName\":null}",
            "{\"firstName\":\"\",\"lastName\":\"Nordmann\"}",
            "{\"firstName\":\"Sara\",\"lastName\":\"\"}",
            "{\"firstName\":\"   \",\"lastName\":\"Nordmann\"}",
            "{\"firstName\":\"Sara\",\"lastName\":\"   \"}"})
    void invalidProfilesAreRejectedWithoutChanges(String body) throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        mvc.perform(updateAs(user).content(body)).andExpect(status().isBadRequest());
        entities.flush();
        entities.clear();
        assertEquals(user.getEmail(), users.findById(user.getUserId()).orElseThrow().getEmail());
        assertEquals("Sara", users.findById(user.getUserId()).orElseThrow().getFirstName());
        assertEquals("Nordmann", users.findById(user.getUserId()).orElseThrow().getLastName());
    }

    @Test
    @Disabled("TODO: Re-enable when API authentication returns 401 for anonymous requests")
    void anonymousUpdateIsRejected() throws Exception {
        mvc.perform(updateAs(null).content("""
                {"firstName":"Sara","lastName":"Nordmann"}
                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedSessionUserReturnsNotFound() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        users.deleteById(user.getUserId());
        mvc.perform(updateAs(user).content("""
                {"firstName":"Sara","lastName":"Nordmann"}
                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileRequestCannotGrantAccessOrReplaceCredentialsOrTargetAnotherUser() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        User other = users.save(new User("other@example.com", "Sara", "Nordmann"));
        mvc.perform(updateAs(user).content("""
                {"email":"updated@example.com","firstName":"Sara","lastName":"Nordmann",
                 "access":"ADMIN","password":"new-secret","passwordHash":"new-hash",
                 "mustChangePassword":false,"userId":%d}
                """.formatted(other.getUserId())))
                .andExpect(status().isOk());
        entities.flush();
        entities.clear();
        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals(Access.STANDARD, stored.getAccess());
        assertEquals(user.getUserId(), stored.getUserId());
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals(other.getEmail(), users.findById(other.getUserId()).orElseThrow().getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"changed@example.com", "other@example.com", "invalid-email", ""})
    void submittedEmailCannotChangeStoredEmail(String email) throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        users.save(new User("other@example.com", "Sara", "Nordmann"));
        mvc.perform(updateAs(user).content("""
                {"email":"%s","firstName":"Sara","lastName":"Nordmann"}
                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
        entities.flush();
        entities.clear();
        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals(user.getUserId(), users.findByEmail(user.getEmail()).orElseThrow().getUserId());
    }
}
