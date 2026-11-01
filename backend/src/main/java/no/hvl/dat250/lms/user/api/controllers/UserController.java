package no.hvl.dat250.lms.user.api.controllers;

import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * Returns the authenticated user's current profile for GET /api/users/me.
     *
     * @param principal the user identified by Spring Security
     * @return the stored profile without the password hash
     * @throws ResponseStatusException with status 401 if no valid user identity is
     *         available, or 404 if the user no longer exists
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal User principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userService.findById(principal.getId())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
