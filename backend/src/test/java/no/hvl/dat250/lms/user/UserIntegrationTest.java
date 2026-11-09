package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {
    @Autowired private UserRepository users;
    @Autowired private EntityManager entities;
    @Autowired private UserService service;

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void serviceCreatesUserWithGeneratedIdentityAndStandardAccess() {
        User administrator = new User("admin@example.com", "System", "Administrator");
        administrator.setAccess(Access.ADMIN);
        users.save(administrator);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(administrator.getUserId()), null, java.util.List.of()));
        User created = service.createUser("Sara", "Nordmann", "sara@example.com");
        Long userId = created.getUserId();
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
        assertEquals("sara@example.com", created.getEmail());
        entities.flush();
        entities.clear();

        User stored = users.findByUserId(userId).orElseThrow();
        assertEquals("Sara", stored.getFirstName());
        assertEquals("Nordmann", stored.getLastName());
        assertEquals(Access.STANDARD, stored.getAccess());
        assertEquals("sara@example.com", stored.getEmail());
        assertEquals(userId, users.findByEmail(stored.getEmail()).orElseThrow().getUserId());
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void userProfileAndAccessSurvivePersistence(Access access) {
        User user = new User("sara@example.com", "Sara", "Nordmann");
        user.setAccess(access);
        users.save(user);
        entities.flush();
        entities.clear();

        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals(user.getUserId(), stored.getUserId());
        assertEquals(user.getEmail(), stored.getName());
        assertEquals("Sara", stored.getFirstName());
        assertEquals("Nordmann", stored.getLastName());
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals(access, stored.getAccess());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void persistenceRejectsMissingFirstName(String firstName) {
        User user = new User("sara@example.com", firstName, "Nordmann");
        assertThrows(ConstraintViolationException.class, () -> {
            users.save(user);
            entities.flush();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void persistenceRejectsMissingLastName(String lastName) {
        User user = new User("sara@example.com", "Sara", lastName);
        assertThrows(ConstraintViolationException.class, () -> {
            users.save(user);
            entities.flush();
        });
    }

    @Test
    void generatedUserIdsAreUniqueAndStableAcrossProfileUpdates() {
        User first = users.save(new User("sara@example.com", "Sara", "Nordmann"));
        User second = users.save(new User("other@example.com", "Sara", "Nordmann"));
        Long userId = first.getUserId();
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
        org.junit.jupiter.api.Assertions.assertNotEquals(userId, second.getUserId());
        first.updateProfile("Kari", "Hansen");
        entities.flush();
        entities.clear();
        User stored = users.findById(userId).orElseThrow();
        assertEquals(userId, stored.getUserId());
        assertEquals(first.getEmail(), stored.getEmail());
        assertEquals(userId, users.findByEmail(first.getEmail()).orElseThrow().getUserId());
        assertEquals("Kari", stored.getFirstName());
        assertEquals("Hansen", stored.getLastName());
    }

}
