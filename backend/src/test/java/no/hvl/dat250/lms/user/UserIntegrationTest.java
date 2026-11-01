package no.hvl.dat250.lms.user;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.fail;
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

    @Test
    @Disabled("Not implemented yet")
    void userApiHandlesValidInvalidAndUnknownUsers() {
        // With MockMvc, test allowed creation/retrieval, invalid data, duplicate
        // email, and an unknown ID against the agreed HTTP contract.
        // Use authorized actors where required; rejected writes must save nothing.
        fail("Not implemented yet");
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
    @Disabled("Not implemented yet")
    void userResponsesNeverExposePasswordHashes() {
        // Verify user detail, list, creation, and error responses do not contain
        // passwords or stored hashes, including for authorized callers.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void userApiEnforcesProfileAndAccessPermissions() {
        // Define profile read/write and global access-management policies.
        // With real security filters, test authorized and unauthorized callers,
        // modified target IDs, and attempts to submit Access in profile requests.
        // Verify rejected changes leave stored profiles and Access unchanged.
        fail("Not implemented yet");
    }
}
