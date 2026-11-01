package no.hvl.dat250.lms.user;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CurrentUserApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;

    @Test
    void returnsAuthenticatedUsersProfileWithoutPassword() throws Exception {
        User user = users.save(new User("{noop}test-password", "Sara", "Nordmann"));
        // A second account must not affect which profile is returned.
        users.save(new User("{noop}other-password", "Sara", "Nordmann"));
        String credentials = Base64.getEncoder().encodeToString(
                (user.getEmail() + ":test-password").getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic " + credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(6)))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.lastName").value("Nordmann"))
                .andExpect(jsonPath("$.email").value(user.getUserId() + "@epost.com"))
                .andExpect(jsonPath("$.access").value("STANDARD"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(content().string(not(containsString("test-password"))));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsIncorrectCredentials() throws Exception {
        User user = users.save(new User("{noop}test-password", "Sara", "Nordmann"));
        String credentials = Base64.getEncoder().encodeToString(
                (user.getEmail() + ":wrong-password").getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic " + credentials))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsNotFoundWhenSessionUserHasBeenDeleted() throws Exception {
        User user = users.save(new User("{noop}test-password", "Sara", "Nordmann"));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        users.deleteById(user.getUserId());

        mvc.perform(get("/api/users/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
