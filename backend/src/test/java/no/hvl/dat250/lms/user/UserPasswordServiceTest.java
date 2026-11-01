package no.hvl.dat250.lms.user;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPasswordServiceTest {
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

    private void authenticate() {
        User principal = mock(User.class);
        when(principal.getUserId()).thenReturn(7L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    @Test
    void changesOnlyAuthenticatedUsersPasswordUsingVerifiedHash() {
        authenticate();
        when(users.replacePasswordIfCurrent(7L, "old-hash", "new-hash")).thenReturn(1);

        service.changePassword("old-hash", "new-hash");

        verify(users).replacePasswordIfCurrent(7L, "old-hash", "new-hash");
        verifyNoMoreInteractions(users);
    }

    @Test
    void anonymousCallerCannotChangePassword() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> service.changePassword("old-hash", "new-hash"));
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void invalidNewHashIsRejectedWithoutWriting(String invalid) {
        authenticate();
        assertThrows(ConstraintViolationException.class,
                () -> service.changePassword("old-hash", invalid));
        verifyNoInteractions(users);
    }

    @Test
    void oversizedNewHashIsRejectedWithoutWriting() {
        authenticate();
        assertThrows(ConstraintViolationException.class,
                () -> service.changePassword("old-hash", "a".repeat(256)));
        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "new-hash"})
    void missingVerificationOrIdenticalHashIsRejected(String expectedHash) {
        authenticate();
        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(expectedHash, "new-hash"));
        verifyNoInteractions(users);
    }

    @Test
    void staleVerificationOrDeletedUserIsRejected() {
        authenticate();
        when(users.replacePasswordIfCurrent(7L, "old-hash", "new-hash")).thenReturn(0);
        assertThrows(AccessDeniedException.class,
                () -> service.changePassword("old-hash", "new-hash"));
    }
}
