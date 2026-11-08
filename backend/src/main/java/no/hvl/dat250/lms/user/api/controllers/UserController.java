package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.Valid;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.api.dto.UserLookupPageResponse;
import no.hvl.dat250.lms.user.domain.AuthenticatedUser;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles HTTP requests for user information.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Finds existing accounts for course managers/admins without exposing administrative fields. */
    // COURSE_MANAGER or higher (ADMIN is included through the role hierarchy).
    @PreAuthorize("hasRole('COURSE_MANAGER')")
    @GetMapping("/search")
    public UserLookupPageResponse searchUsers(@RequestParam String search,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return UserLookupPageResponse.from(userService.searchUsersForEnrollment(search, page, size));
    }

    /**
     * Returns the authenticated user's current profile for GET /api/users/me.
     *
     * @param principal the caller's user ID, identified by Spring Security
     * @return the stored application profile
     * @throws ResponseStatusException with status 401 if no valid user identity is
     *         available, or 404 if the user no longer exists
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userService.findById(principal.userId())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** Replaces the authenticated user's names. User ID and email are immutable. */
    @PutMapping("/me")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @Valid @RequestBody UpdateUserRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return UserResponse.from(userService.updateProfile(principal.userId(), request));
    }
}
