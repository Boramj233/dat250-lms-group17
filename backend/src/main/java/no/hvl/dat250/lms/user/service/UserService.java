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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Manages local profiles and application access; identity-provider login belongs to auth. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Validator validator;

    /** Creates the service with its user repository. */
    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    /**
     * Creates a STANDARD local profile for an externally managed account.
     * Only an authenticated user whose stored access is ADMIN may call this method.
     * Only the local ID is generated. Email must match the external identity.
     *
     * @param firstName required first name, at most 255 characters
     * @param lastName required last name, at most 255 characters
     * @param email required identity-provider email, at most 255 characters
     * @return the saved profile with its generated ID
     * @throws ConstraintViolationException if names or email are invalid
     */
    @Transactional
    public User createUser(String firstName, String lastName, String email) {
        requireAdministrator();
        User user = new User(email, firstName, lastName);
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException();
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

    /** Reloads the authenticated account, for trusted internal callers. */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return userRepository.findById(requireCurrentUserId()).orElseThrow(UserNotFoundException::new);
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
     * Replaces names without changing userId, email, or global access.
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

}
