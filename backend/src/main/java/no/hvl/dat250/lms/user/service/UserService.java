package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserLookupResponse;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.domain.Access;
import java.util.Optional;
import java.util.Arrays;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    private User requireAdministrator() {
        return requireAccess(Access.ADMIN);
    }

    /** Authorizes against current persisted global access, never the session's old role. */
    private User requireAccess(Access... allowedAccess) {
        Long currentUserId = requireCurrentUserId();
        User actor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Operation not permitted"));
        if (!Arrays.asList(allowedAccess).contains(actor.getAccess())) {
            throw new AccessDeniedException("Operation not permitted");
        }
        return actor;
    }

    private Long requireCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User principal)
                || principal.getUserId() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal.getUserId();
    }

    /**
     * Persists an auth-verified password change for the currently authenticated user.
     * Auth must verify the old plaintext password, reject reuse, and encode the new
     * password before calling this internal method. Both arguments are stored/encoded
     * hashes, never plaintext. No target ID is accepted from the caller.
     *
     * The expected hash prevents an older verification from overwriting a newer
     * password change. The new hash and first-login flag are updated atomically.
     * Auth must invalidate/refresh sessions after success; this does not update a
     * detached principal or expose a password-change HTTP endpoint.
     */
    @Transactional
    public void changePassword(String verifiedCurrentPasswordHash, String newPasswordHash) {
        Long userId = requireCurrentUserId();
        var violations = validator.validateValue(User.class, "passwordHash", newPasswordHash);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        if (verifiedCurrentPasswordHash == null || verifiedCurrentPasswordHash.isBlank()
                || verifiedCurrentPasswordHash.equals(newPasswordHash)) {
            throw new IllegalArgumentException("A different encoded password and verified current hash are required");
        }
        if (userRepository.replacePasswordIfCurrent(userId, verifiedCurrentPasswordHash, newPasswordHash) != 1) {
            throw new AccessDeniedException("Password change could not be completed");
        }
    }

    /** Lists/searches users for administrators, with bounded pages sorted by ID. */
    @Transactional(readOnly = true)
    public Page<User> listUsers(String search, int page, int size) {
        requireAdministrator();
        return findUsers(search, page, size);
    }

    /** Restricted account lookup for enrollment; this does not create accounts or enroll users. */
    @Transactional(readOnly = true)
    public Page<UserLookupResponse> searchUsersForEnrollment(String search, int page, int size) {
        requireAccess(Access.ADMIN, Access.COURSE_MANAGER);
        if (search == null || search.isBlank()) {
            throw new IllegalArgumentException("A non-blank search is required");
        }
        return findUsers(search, page, size).map(UserLookupResponse::from);
    }

    /** Shared bounded search; public callers authorize before invoking this helper. */
    private Page<User> findUsers(String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        String query = search == null ? "" : search.strip();
        if (query.length() > 255) {
            throw new IllegalArgumentException("Search must be at most 255 characters");
        }
        var pageable = PageRequest.of(page, size, Sort.by("userId"));
        if (query.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        Long searchedId = null;
        try {
            searchedId = Long.valueOf(query);
        } catch (NumberFormatException ignored) {
            // Non-numeric searches only match name/email fields.
        }
        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUserId(
                        query, query, query, searchedId, pageable);
    }

    /** Returns another user's profile only after checking administrator access. */
    @Transactional(readOnly = true)
    public User getUserForAdministrator(Long userId) {
        requireAdministrator();
        return userRepository.findByUserId(userId).orElseThrow(UserNotFoundException::new);
    }

    /** Administrators may assign global access, but cannot remove their own ADMIN access. */
    @Transactional
    public User updateAccess(Long userId, Access access) {
        User actor = requireAdministrator();
        if (access == null) {
            throw new IllegalArgumentException("Access is required");
        }
        if (actor.getUserId().equals(userId) && access != Access.ADMIN) {
            throw new AccessDeniedException("Cannot remove your own administrator access");
        }
        User target = userRepository.findByUserId(userId).orElseThrow(UserNotFoundException::new);
        target.setAccess(access);
        return userRepository.save(target);
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
