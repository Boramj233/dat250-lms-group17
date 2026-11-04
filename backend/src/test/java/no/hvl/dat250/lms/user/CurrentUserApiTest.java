package no.hvl.dat250.lms.user;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
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

import static org.hamcrest.Matchers.aMapWithSize;
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
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        // A second account must not affect which profile is returned.
        users.save(new User("other@example.com", "Sara", "Nordmann"));
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .session(sessionFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.lastName").value("Nordmann"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.access").value("STANDARD"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @Disabled("TODO: Re-enable when API authentication returns 401 for anonymous requests")
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(user, null, java.util.List.of())));
        return session;
    }

    @Test
    void returnsNotFoundWhenSessionUserHasBeenDeleted() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, java.util.List.of());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        users.deleteById(user.getUserId());

        mvc.perform(get("/api/users/me").session(session).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
