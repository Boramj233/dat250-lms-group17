package no.hvl.dat250.lms.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.service.CannotRemoveOwnAdminAccessException;
import no.hvl.dat250.lms.user.service.UserAlreadyExistsException;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The profile rules of UserService. Who may call it (ADMIN, COURSE_MANAGER) is checked
 * by the security feature before the service runs; see AdminUserApiTest.
 */
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

    @Test
    void userCanBeCreatedWithValidDetails() {
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = service.createUser("Sara", "Nordmann", "sara@example.com");

        assertEquals("Sara", user.getFirstName());
        assertEquals("Nordmann", user.getLastName());
        assertEquals("sara@example.com", user.getEmail());
        assertEquals(Access.STANDARD, user.getAccess());
        verify(users).save(user);
    }

    @Test
    void emailIsStoredInLowerCaseAsKeycloakReportsIt() {
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals("sara@example.com", service.createUser("Sara", "Nordmann", " Sara@Example.COM ").getEmail());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidUserDetailsAreRejected(String invalid) {
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser(invalid, "Nordmann", "sara@example.com"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", invalid, "sara@example.com"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", "Nordmann", invalid));
        verify(users, never()).save(any());
    }

    @Test
    void oversizedUserDetailsAreRejected() {
        String oversized = "a".repeat(256);
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser(oversized, "Nordmann", "sara@example.com"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", oversized, "sara@example.com"));
        assertThrows(ConstraintViolationException.class,
                () -> service.createUser("Sara", "Nordmann", oversized));
        verify(users, never()).save(any());
    }

    @Test
    void existingExternalEmailCannotCreateAnotherProfile() {
        when(users.findByEmail("sara@example.com"))
                .thenReturn(Optional.of(new User("sara@example.com", "Sara", "Nordmann")));
        assertThrows(UserAlreadyExistsException.class,
                () -> service.createUser("Other", "Profile", "sara@example.com"));
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Access.class, names = {"STANDARD", "COURSE_MANAGER"})
    void adminCannotRemoveTheirOwnAdminAccess(Access access) {
        assertThrows(CannotRemoveOwnAdminAccessException.class,
                () -> service.updateAccess(1L, 1L, access));
        verifyNoInteractions(users);
    }

    @Test
    void adminCanChangeAnotherUsersAccess() {
        User target = new User("ola@example.com", "Ola", "Nordmann");
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);

        assertEquals(Access.COURSE_MANAGER, service.updateAccess(1L, 2L, Access.COURSE_MANAGER).getAccess());
    }
}
