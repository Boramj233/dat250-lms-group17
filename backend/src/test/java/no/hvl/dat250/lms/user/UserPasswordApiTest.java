package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPasswordApiTest {
    private static final String OLD_PASSWORD = "old-password123";
    private static final String NEW_PASSWORD = "new-password456";
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private EntityManager entities;
    private User user;
    private MockHttpSession session;
    private String originalHash;

    @BeforeEach
    void createUser() {
        originalHash = passwords.encode(OLD_PASSWORD);
        user = users.save(new User(originalHash, "Sara", "Nordmann"));
    }

    private MockHttpServletRequestBuilder changeAs(User actor) {
        session = new MockHttpSession();
        if (actor != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                            actor, null, actor.getAuthorities())));
        }
        var token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "password-test-token");
        var request = new MockHttpServletRequest();
        request.setSession(session);
        var response = new MockHttpServletResponse();
        new HttpSessionCsrfTokenRepository().saveToken(token, request, response);
        new XorCsrfTokenRequestAttributeHandler().handle(request, response, () -> token);
        CsrfToken exposed = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return put("/api/users/me/password").session(session)
                .header(token.getHeaderName(), exposed.getToken())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
    }

    private String validBody() {
        return "{\"currentPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}";
    }

    private String basicCredentials(String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (user.getEmail() + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private void assertCredentialsUnchanged() {
        entities.flush();
        entities.clear();
        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        assertEquals(originalHash, stored.getPassword());
        assertTrue(stored.isMustChangePassword());
    }

    @Test
    void changesPasswordClearsRequirementLogsOutAndAuthenticatesOnlyWithNewPassword() throws Exception {
        User other = users.save(new User(passwords.encode("other-password"), "Ola", "Hansen"));
        String otherHash = other.getPassword();
        mvc.perform(changeAs(user).content(validBody()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertTrue(session.isInvalid());
        entities.flush();
        entities.clear();
        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        assertFalse(stored.isMustChangePassword());
        assertNotEquals(NEW_PASSWORD, stored.getPassword());
        assertNotEquals(originalHash, stored.getPassword());
        assertTrue(passwords.matches(NEW_PASSWORD, stored.getPassword()));
        assertFalse(passwords.matches(OLD_PASSWORD, stored.getPassword()));
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals(otherHash, users.findByUserId(other.getUserId()).orElseThrow().getPassword());

        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", basicCredentials(OLD_PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", basicCredentials(NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void wrongCurrentPasswordCannotChangeCredentials() throws Exception {
        mvc.perform(changeAs(user).content("""
                {"currentPassword":"wrong-password","newPassword":"new-password456"}
                """))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("wrong-password"))))
                .andExpect(content().string(not(containsString(originalHash))));
        assertCredentialsUnchanged();
        assertFalse(session.isInvalid());
    }

    @Test
    void samePlaintextPasswordIsRejected() throws Exception {
        mvc.perform(changeAs(user).content("""
                {"currentPassword":"old-password123","newPassword":"old-password123"}
                """))
                .andExpect(status().isBadRequest());
        assertCredentialsUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}",
            "{\"newPassword\":\"new-password456\"}",
            "{\"currentPassword\":\"old-password123\"}",
            "{\"currentPassword\":\"\",\"newPassword\":\"new-password456\"}",
            "{\"currentPassword\":\"old-password123\",\"newPassword\":null}",
            "{\"currentPassword\":\"old-password123\",\"newPassword\":\"short\"}",
            "{\"currentPassword\":\"old-password123\",\"newPassword\":\"        \"}"})
    void invalidRequestsAreRejectedWithoutCredentialChanges(String body) throws Exception {
        mvc.perform(changeAs(user).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(OLD_PASSWORD))));
        assertCredentialsUnchanged();
    }

    @Test
    void rejectsPasswordsExceedingBcryptByteLimit() throws Exception {
        for (String password : new String[]{"a".repeat(73), "\u00f8".repeat(37)}) {
            mvc.perform(changeAs(user).content("{\"currentPassword\":\"" + OLD_PASSWORD
                            + "\",\"newPassword\":\"" + password + "\"}"))
                    .andExpect(status().isBadRequest());
            assertCredentialsUnchanged();
        }
    }

    @Test
    void anonymousAndCsrfLessRequestsAreRejected() throws Exception {
        mvc.perform(changeAs(null).content(validBody())).andExpect(status().isUnauthorized());
        changeAs(user);
        mvc.perform(put("/api/users/me/password").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
        assertCredentialsUnchanged();
    }

    @Test
    void staleSessionHashCannotVerifyOldPassword() throws Exception {
        entities.flush();
        entities.detach(user);
        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        stored.changePassword(passwords.encode("different-password"));
        entities.flush();
        String replacementHash = stored.getPassword();
        mvc.perform(changeAs(user).content(validBody())).andExpect(status().isForbidden());
        entities.clear();
        assertEquals(replacementHash, users.findByUserId(user.getUserId()).orElseThrow().getPassword());
    }

    @Test
    void requestCannotSelectAnotherAccount() throws Exception {
        User other = users.save(new User(passwords.encode(OLD_PASSWORD), "Ola", "Hansen"));
        String otherHash = other.getPassword();
        mvc.perform(changeAs(user).content(validBody().replace("}", ",\"userId\":" + other.getUserId() + "}")))
                .andExpect(status().isNoContent());
        entities.clear();
        assertEquals(otherHash, users.findByUserId(other.getUserId()).orElseThrow().getPassword());
        assertTrue(passwords.matches(NEW_PASSWORD, users.findByUserId(user.getUserId()).orElseThrow().getPassword()));
    }
}
