package no.hvl.dat250.lms.user;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {
    @Test
    @Disabled("Not implemented yet")
    void userProfileAndAccessSurvivePersistence() {
        // Persist a user, flush/clear, and reload it. Verify identity, email,
        // profile fields, and Access. Test each Access enum value separately.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void userApiHandlesValidInvalidAndUnknownUsers() {
        // With MockMvc, test allowed creation/retrieval, invalid data, duplicate
        // email, and an unknown ID against the agreed HTTP contract.
        // Use authorized actors where required; rejected writes must save nothing.
        fail("Not implemented yet");
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
