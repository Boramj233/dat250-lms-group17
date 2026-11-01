package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.Valid;
import no.hvl.dat250.lms.user.api.dto.UpdateAccessRequest;
import no.hvl.dat250.lms.user.api.dto.UserPageResponse;
import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.web.bind.annotation.*;

/** Administrator operations; authorization is enforced in the service for every call. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService users;

    public AdminUserController(UserService users) { this.users = users; }

    @GetMapping
    public UserPageResponse listUsers(@RequestParam(defaultValue = "") String search,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return UserPageResponse.from(users.listUsers(search, page, size));
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return UserResponse.from(users.getUserForAdministrator(userId));
    }

    @PatchMapping("/{userId}/access")
    public UserResponse updateAccess(@PathVariable Long userId,
                                     @Valid @RequestBody UpdateAccessRequest request) {
        return UserResponse.from(users.updateAccess(userId, request.access()));
    }
}
