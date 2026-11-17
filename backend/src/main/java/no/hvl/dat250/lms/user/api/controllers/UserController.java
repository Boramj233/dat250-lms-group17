package no.hvl.dat250.lms.user.api.controllers;

import java.util.List;
import jakarta.validation.Valid;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.annotations.IsCourseManager;
import no.hvl.dat250.lms.user.api.dto.UpcomingDeadlineResponse;
import no.hvl.dat250.lms.user.api.dto.UpdateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UserGradeResponse;
import no.hvl.dat250.lms.user.api.dto.UserLookupPageResponse;
import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.service.UserDashboardService;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own profile, their overview across courses (grades, upcoming deadlines),
 * and account lookup for enrollment.
 *
 * <p>Every /api request is authenticated by the security feature before it gets here,
 * so the principal is always present.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserDashboardService dashboardService;

    public UserController(UserService userService, UserDashboardService dashboardService) {
        this.userService = userService;
        this.dashboardService = dashboardService;
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

    /** The caller's handed-in assignments and coding exercises across all courses, with released grades. */
    @GetMapping("/me/grades")
    public List<UserGradeResponse> getMyGrades(@AuthenticationPrincipal AuthenticatedUser principal) {
        return dashboardService.grades(principal.userId());
    }

    /** Deadlines still ahead in the user's courses, soonest first. Only the user themselves or an admin. */
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    @GetMapping("/{userId}/upcoming-deadlines")
    public List<UpcomingDeadlineResponse> getUpcomingDeadlines(@PathVariable Long userId) {
        return dashboardService.upcomingDeadlines(userId);
    }
}
