package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.Valid;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.annotations.IsCourseManager;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserLookupPageResponse;
import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own profile, and account lookup for enrollment.
 *
 * <p>Every /api request is authenticated by the security feature before it gets here,
 * so the principal is always present.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Finds existing accounts for course managers/admins without exposing administrative fields. */
    @IsCourseManager
    @GetMapping("/search")
    public UserLookupPageResponse searchUsers(@RequestParam String search,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return UserLookupPageResponse.from(userService.searchUsersForEnrollment(search, page, size));
    }

    /** The caller's current profile; 404 if it was removed. */
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(userService.getUser(principal.userId()));
    }

    /** Replaces the caller's names. User ID, email and access are immutable here. */
    @PutMapping("/me")
    public UserResponse updateCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.updateProfile(principal.userId(), request));
    }
}
