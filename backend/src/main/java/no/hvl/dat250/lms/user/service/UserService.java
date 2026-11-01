package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.domain.Access;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates users and provides profile operations and Spring Security user lookups. */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Validator validator;

    /** Creates the service with its user repository. */
    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    /**
     * Creates a STANDARD user with a required first-login password change.
     * Only an authenticated user whose stored access is ADMIN may call this method.
     * ID and email are generated when the user is persisted.
     * The trusted caller (for example, AuthService) must encode the password first;
     * this method stores the supplied hash unchanged and does not verify its format.
     *
     * @param firstName required first name, at most 255 characters
     * @param lastName required last name, at most 255 characters
     * @param passwordHash required, already encoded password, at most 255 characters
     * @return the saved user, including generated ID and email
     * @throws ConstraintViolationException if names or password hash are invalid
     */
    @Transactional
    public User createUser(String firstName, String lastName, String passwordHash) {
        requireAdministrator();
        User user = new User(passwordHash, firstName, lastName);
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return userRepository.save(user);
    }

    /** Reads current stored access so an outdated session cannot retain admin privileges. */
    private void requireAdministrator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User principal)
                || principal.getUserId() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        User actor = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Administrator access required"));
        if (actor.getAccess() != Access.ADMIN) {
            throw new AccessDeniedException("Administrator access required");
        }
    }

    /** Returns the user with the given ID, or an empty Optional if not found. */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Replaces names without changing userId, email, password, or global access.
     * The caller must obtain the target ID from the authenticated identity.
     */
    @Transactional
    public User updateProfile(Long id, UpdateUserRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.updateProfile(request.firstName(), request.lastName());
        return userRepository.save(user);
    }

    /**
     * Loads an account by email for Spring Security, despite the interface method name.
     * This lookup does not verify the password.
     *
     * @param email the email used to identify the account
     * @return the stored user details
     * @throws UsernameNotFoundException if the email is unknown
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }

}
