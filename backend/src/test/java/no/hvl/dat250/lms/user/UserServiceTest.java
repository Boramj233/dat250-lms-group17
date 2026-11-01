package no.hvl.dat250.lms.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static ValidatorFactory validators;
    @Mock private UserRepository users;
    private UserService service;

    @BeforeAll
    static void openValidator() { validators = Validation.buildDefaultValidatorFactory(); }

    @AfterAll
    static void closeValidator() { validators.close(); }

    @BeforeEach
    void setup() { service = new UserService(users, validators.getValidator()); }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void authenticateAs(Access storedAccess) {
        User principal = mock(User.class);
        when(principal.getUserId()).thenReturn(1L);
        User stored = new User("encoded-password", "Admin", "User");
        stored.setAccess(storedAccess);
        when(users.findById(1L)).thenReturn(Optional.of(stored));
        // Even an ADMIN authority in the session must not override stored access.
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void userCanBeCreatedWithValidDetails() {
        authenticateAs(Access.ADMIN);
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = service.createUser("Sara", "Nordmann", "encoded-password");

        assertEquals("Sara", user.getFirstName());
        assertEquals("Nordmann", user.getLastName());
        assertEquals("encoded-password", user.getPassword());
        assertEquals(Access.STANDARD, user.getAccess());
        assertTrue(user.isMustChangePassword());
        verify(users).save(user);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidUserDetailsAreRejected(String invalid) {
        authenticateAs(Access.ADMIN);
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser(invalid, "Nordmann", "encoded-password"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", invalid, "encoded-password"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", "Nordmann", invalid));
        verify(users, never()).save(any());
    }

    @Test
    void oversizedUserDetailsAreRejected() {
        authenticateAs(Access.ADMIN);
        String oversized = "a".repeat(256);
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser(oversized, "Nordmann", "encoded-password"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", oversized, "encoded-password"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", "Nordmann", oversized));
        verify(users, never()).save(any());
    }

    @Test
    void anonymousCallerCannotCreateUsers() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> service.createUser("Sara", "Nordmann", "encoded-password"));
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @EnumSource(value = Access.class, names = {"STANDARD", "COURSE_MANAGER"})
    void storedNonAdministratorCannotCreateUsersEvenWithStaleAdminAuthority(Access access) {
        authenticateAs(access);
        assertThrows(AccessDeniedException.class,
                () -> service.createUser("Sara", "Nordmann", "encoded-password"));
        verify(users, never()).save(any());
    }

    @Test
    void deletedSessionUserCannotCreateUsers() {
        User principal = mock(User.class);
        when(principal.getUserId()).thenReturn(99L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        assertThrows(AccessDeniedException.class,
                () -> service.createUser("Sara", "Nordmann", "encoded-password"));
        verify(users, never()).save(any());
    }

    @Test
    @Disabled("Not implemented yet")
    void existingUserCanBeRetrieved() {
        // Retrieve the requested user by ID and verify the returned profile data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void unknownUserIsHandledExplicitly() {
        // Request an unknown user ID and verify the agreed not-found outcome.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void profileChangesDoNotGrantGlobalAccess() {
        // An ordinary profile update must not allow elevation to COURSE_MANAGER
        // or ADMIN. Keep global access changes separate from editable profile data.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void globalAccessChangesRequireExplicitAuthorization() {
        // Define who may assign STANDARD, COURSE_MANAGER, and ADMIN, then test
        // permitted and rejected actors. Do not infer this policy from course roles.
        // Rejected changes must leave the user's current Access unchanged.
        fail("Not implemented yet");
    }
}
