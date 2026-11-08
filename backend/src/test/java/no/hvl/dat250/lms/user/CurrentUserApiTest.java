package no.hvl.dat250.lms.user;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.auth.ApiTokenService;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
    @Autowired private ApiTokenService tokens;
    @Autowired private UserRepository users;

    @Test
    void returnsAuthenticatedUsersProfileWithoutPassword() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        // A second account must not affect which profile is returned.
        users.save(new User("other@example.com", "Sara", "Nordmann"));
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokens.issue(user).accessToken()))
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
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void deletedTokenUserIsUnauthenticated() throws Exception {
        User user = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        String token = tokens.issue(user).accessToken();
        users.deleteById(user.getUserId());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
