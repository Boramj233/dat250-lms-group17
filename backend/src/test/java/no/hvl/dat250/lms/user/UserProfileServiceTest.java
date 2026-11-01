package no.hvl.dat250.lms.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
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
    void updatesNamesWithoutChangingPasswordOrAccess() {
        User user = new User("encoded-password", "Sara", "Nordmann");
        user.setAccess(Access.COURSE_MANAGER);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        User updated = service.updateProfile(1L, new UpdateUserRequest("Kari", "Hansen"));

        assertEquals("Kari", updated.getFirstName());
        assertEquals("Hansen", updated.getLastName());
        assertEquals("encoded-password", updated.getPassword());
        assertEquals(Access.COURSE_MANAGER, updated.getAccess());
        verify(users).save(user);
    }

    @Test
    void rejectsFieldsLongerThanDatabaseColumns() {
        assertThrows(ConstraintViolationException.class, () -> service.updateProfile(1L,
                new UpdateUserRequest("a".repeat(256), "Nordmann")));
        assertThrows(ConstraintViolationException.class, () -> service.updateProfile(1L,
                new UpdateUserRequest("Sara", "a".repeat(256))));
        verifyNoInteractions(users);
    }

    @Test
    void unknownUserIsNotSaved() {
        assertThrows(UserNotFoundException.class,
                () -> service.updateProfile(99L, new UpdateUserRequest("Sara", "Nordmann")));
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void eachNameIsRequiredBeforeAccessingDatabase(String name) {
        assertThrows(ConstraintViolationException.class,
                () -> service.updateProfile(1L, new UpdateUserRequest(name, "Nordmann")));
        assertThrows(ConstraintViolationException.class,
                () -> service.updateProfile(1L, new UpdateUserRequest("Sara", name)));
        verifyNoInteractions(users);
    }
}
