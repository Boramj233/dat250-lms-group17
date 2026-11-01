package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
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
        User administrator = new User("encoded-admin-password", "System", "Administrator");
        administrator.setAccess(Access.ADMIN);
        users.save(administrator);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(administrator, null, administrator.getAuthorities()));
        User created = service.createUser("Sara", "Nordmann", "encoded-password");
        Long userId = created.getUserId();
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
        assertEquals(userId + "@epost.com", created.getEmail());
        entities.flush();
        entities.clear();

        User stored = users.findByUserId(userId).orElseThrow();
        assertEquals("Sara", stored.getFirstName());
        assertEquals("Nordmann", stored.getLastName());
        assertEquals("encoded-password", stored.getPassword());
        assertEquals(Access.STANDARD, stored.getAccess());
        org.junit.jupiter.api.Assertions.assertTrue(stored.isMustChangePassword());
        assertEquals(userId + "@epost.com", stored.getEmail());
        assertEquals(userId, users.findByEmail(stored.getEmail()).orElseThrow().getUserId());
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void userProfileAndAccessSurvivePersistence(Access access) {
        User user = new User("encoded-password", "Sara", "Nordmann");
        user.setAccess(access);
        users.save(user);
        entities.flush();
        entities.clear();

        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals(user.getUserId(), stored.getUserId());
        assertEquals(user.getUserId() + "@epost.com", stored.getUsername());
        assertEquals("Sara", stored.getFirstName());
        assertEquals("Nordmann", stored.getLastName());
        assertEquals(user.getUserId() + "@epost.com", stored.getEmail());
        assertEquals("encoded-password", stored.getPassword());
        assertEquals(access, stored.getAccess());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void persistenceRejectsMissingFirstName(String firstName) {
        User user = new User("encoded-password", firstName, "Nordmann");
        assertThrows(ConstraintViolationException.class, () -> {
            users.save(user);
            entities.flush();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void persistenceRejectsMissingLastName(String lastName) {
        User user = new User("encoded-password", "Sara", lastName);
        assertThrows(ConstraintViolationException.class, () -> {
            users.save(user);
            entities.flush();
        });
    }

    @Test
    void generatedUserIdsAreUniqueAndStableAcrossProfileUpdates() {
        User first = users.save(new User("encoded-password", "Sara", "Nordmann"));
        User second = users.save(new User("encoded-password", "Sara", "Nordmann"));
        Long userId = first.getUserId();
        org.junit.jupiter.api.Assertions.assertNotNull(userId);
        org.junit.jupiter.api.Assertions.assertNotEquals(userId, second.getUserId());
        assertEquals(userId + "@epost.com", first.getEmail());
        assertEquals(second.getUserId() + "@epost.com", second.getEmail());
        first.updateProfile("Kari", "Hansen");
        entities.flush();
        entities.clear();
        User stored = users.findById(userId).orElseThrow();
        assertEquals(userId, stored.getUserId());
        assertEquals(userId + "@epost.com", stored.getEmail());
        assertEquals(userId, users.findByEmail(userId + "@epost.com").orElseThrow().getUserId());
        assertEquals("Kari", stored.getFirstName());
        assertEquals("Hansen", stored.getLastName());
    }

    @Test
    void passwordHashAndFirstLoginFlagAreReplacedTogether() {
        User user = users.save(new User("temporary-hash", "Sara", "Nordmann"));
        org.junit.jupiter.api.Assertions.assertTrue(user.isMustChangePassword());
        user.changePassword("new-encoded-password");
        entities.flush();
        entities.clear();
        User stored = users.findById(user.getUserId()).orElseThrow();
        assertEquals("new-encoded-password", stored.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(stored.isMustChangePassword());
        assertEquals(user.getEmail(), stored.getEmail());
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void servicePersistsPasswordChangeForOwnAccountAndPreservesOtherFields(Access access) {
        User user = new User("temporary-hash", "Sara", "Nordmann");
        user.setAccess(access);
        users.save(user);
        User other = users.save(new User("other-hash", "Ola", "Hansen"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));

        service.changePassword("temporary-hash", "new-encoded-password");
        entities.flush();
        entities.clear();

        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        assertEquals("new-encoded-password", stored.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(stored.isMustChangePassword());
        assertEquals(user.getEmail(), stored.getEmail());
        assertEquals("Sara", stored.getFirstName());
        assertEquals("Nordmann", stored.getLastName());
        assertEquals(access, stored.getAccess());
        User storedOther = users.findByUserId(other.getUserId()).orElseThrow();
        assertEquals("other-hash", storedOther.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(storedOther.isMustChangePassword());
    }

    @Test
    void oldVerifiedHashCannotOverwriteANewerPasswordChange() {
        User user = users.save(new User("old-hash", "Sara", "Nordmann"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        service.changePassword("old-hash", "first-new-hash");

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.changePassword("old-hash", "second-new-hash"));

        User stored = users.findByUserId(user.getUserId()).orElseThrow();
        assertEquals("first-new-hash", stored.getPassword());
        org.junit.jupiter.api.Assertions.assertFalse(stored.isMustChangePassword());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidReplacementHashDoesNotClearFirstLoginRequirement(String hash) {
        User user = new User("temporary-hash", "Sara", "Nordmann");
        assertThrows(IllegalArgumentException.class, () -> user.changePassword(hash));
        assertEquals("temporary-hash", user.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(user.isMustChangePassword());
    }

}
