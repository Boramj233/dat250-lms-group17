package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserImportResponse;
import no.hvl.dat250.lms.user.api.dto.UserLookupResponse;
import no.hvl.dat250.lms.user.repository.UserRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.domain.Access;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Manages local profiles and global access.
 *
 * <p>Who may call what is decided by the security feature before a method here runs
 * (the controllers' {@code @IsAdmin}/{@code @IsCourseManager}, with access read from the
 * database on every request). This service only enforces the rules of the profiles
 * themselves: validation, unique emails, and that an admin cannot demote themselves.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Validator validator;

    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }

    /**
     * Creates a STANDARD local profile for an externally managed account. Only the local
     * ID is generated. The email must match the external identity; it is stored in lower
     * case, as Keycloak reports emails at login.
     *
     * @throws ConstraintViolationException if names or email are invalid
     * @throws UserAlreadyExistsException if the email already has a profile
     */
    @Transactional
    public User createUser(String firstName, String lastName, String email) {
        User user = new User(normalizeEmail(email), firstName, lastName);
        var violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException();
        }
        return userRepository.save(user);
    }

    /**
     * Creates local profiles for a list of users, e.g. a class list. Each row is checked
     * like {@link #createUser} (same validation of email and names), plus its access
     * level (blank = STANDARD). Rows with an email that already has a profile are skipped
     * and left unchanged, so the same file can be uploaded again. Invalid rows are
     * reported and do not stop the other rows.
     *
     * <p>Only profiles are created, never login accounts: the users register in Keycloak
     * themselves with the same email, and their first login finds the profile.
     */
    @Transactional
    public UserImportResponse importUsers(List<UserImportRow> rows) {
        List<UserImportResponse.Row> report = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (UserImportRow row : rows) {
            String email = normalizeEmail(row.email());
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

    /** Lists/searches users, with bounded pages sorted by ID. */
    @Transactional(readOnly = true)
    public Page<User> listUsers(String search, int page, int size) {
        return findUsers(search, page, size);
    }

    /** Account lookup for enrollment: only identity fields, and a search is required. */
    @Transactional(readOnly = true)
    public Page<UserLookupResponse> searchUsersForEnrollment(String search, int page, int size) {
        if (search == null || search.isBlank()) {
            throw new IllegalArgumentException("A non-blank search is required");
        }
        return findUsers(search, page, size).map(UserLookupResponse::from);
    }

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

    /**
     * Returns the profile with this ID.
     *
     * @throws UserNotFoundException if there is none
     */
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    /**
     * Sets a user's global access.
     *
     * @param actorId the admin making the change; an admin cannot remove their own ADMIN
     *        access, so there is always at least one admin left
     * @throws CannotRemoveOwnAdminAccessException if the actor tries to demote themselves
     * @throws UserNotFoundException if the target does not exist
     */
    @Transactional
    public User updateAccess(Long actorId, Long userId, Access access) {
        if (access == null) {
            throw new IllegalArgumentException("Access is required");
        }
        if (userId.equals(actorId) && access != Access.ADMIN) {
            throw new CannotRemoveOwnAdminAccessException();
        }
        User target = getUser(userId);
        target.setAccess(access);
        return userRepository.save(target);
    }

    /**
     * For the login (security feature) only: the email must come from a verified OIDC
     * identity. {@code initialAccess} is used only when the profile is created; an
     * existing profile's access is never changed here.
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

    /** Replaces names without changing userId, email, or global access. */
    @Transactional
    public User updateProfile(Long id, UpdateUserRequest request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        User user = getUser(id);
        user.updateProfile(request.firstName(), request.lastName());
        return userRepository.save(user);
    }

    /** Emails are compared and stored in lower case, as Keycloak reports them. */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
