package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserImportResponse;
import no.hvl.dat250.lms.user.api.dto.UserLookupResponse;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Administrator only: creates local profiles for a list of users, e.g. a class list.
     * Each row is checked like {@link #createUser} (same validation of email and names),
     * plus its access level (blank = STANDARD). Emails are stored in lower case, as
     * Keycloak reports them at login. Rows with an email that already has a profile are
     * skipped and left unchanged, so the same file can be uploaded again. Invalid rows
     * are reported and do not stop the other rows.
     *
     * <p>Only profiles are created, never login accounts: the users register in Keycloak
     * themselves with the same email, and their first login finds the profile.
     */
    @Transactional
    public UserImportResponse importUsers(List<UserImportRow> rows) {
        requireAdministrator();
        List<UserImportResponse.Row> report = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (UserImportRow row : rows) {
            String email = row.email() == null ? "" : row.email().trim().toLowerCase(Locale.ROOT);
            List<String> errors = new ArrayList<>();

            Access access = Access.STANDARD;
            if (row.access() != null && !row.access().isBlank()) {
                try {
                    access = Access.valueOf(row.access().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    errors.add("access: must be one of " + Arrays.toString(Access.values()));
                }
            }
            User user = new User(email, trimmed(row.firstName()), trimmed(row.lastName()));
            user.setAccess(access);
            validator.validate(user).stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .forEach(errors::add);

            UserImportResponse.Row result;
            if (!errors.isEmpty()) {
                result = new UserImportResponse.Row(row.line(), email, UserImportResponse.Status.INVALID, null, errors);
                failed++;
            } else if (!seen.add(email)) {
                result = new UserImportResponse.Row(row.line(), email,
                        UserImportResponse.Status.DUPLICATE_IN_FILE, null, List.of());
                failed++;
            } else {
                var existing = userRepository.findByEmail(email);
                if (existing.isPresent()) {
                    result = new UserImportResponse.Row(row.line(), email,
                            UserImportResponse.Status.ALREADY_EXISTS, existing.get().getUserId(), List.of());
                    skipped++;
                } else {
                    User saved = userRepository.save(user);
                    result = new UserImportResponse.Row(row.line(), email,
                            UserImportResponse.Status.CREATED, saved.getUserId(), List.of());
                    created++;
                }
            }
            report.add(result);
        }
        return new UserImportResponse(created, skipped, failed, report);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
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
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal.userId();
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

    /** Trusted auth callers only: the email must come from a verified OIDC identity. */
    @Transactional
    public User findOrCreateOidcUser(String email, String firstName, String lastName) {
        return findOrCreateOidcUser(email, firstName, lastName, Access.STANDARD);
    }

    /**
     * Trusted auth callers only: the email must come from a verified OIDC identity.
     * {@code initialAccess} is used only when the profile is created; an existing
     * profile's access is never changed here.
     */
    @Transactional
    public User findOrCreateOidcUser(String email, String firstName, String lastName,
                                     Access initialAccess) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User(email, firstName, lastName);
            user.setAccess(initialAccess);
            var violations = validator.validate(user);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return userRepository.saveAndFlush(user);
        });
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
