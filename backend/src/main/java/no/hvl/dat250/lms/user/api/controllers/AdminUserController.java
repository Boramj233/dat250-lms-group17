package no.hvl.dat250.lms.user.api.controllers;

import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.annotations.IsAdmin;
import no.hvl.dat250.lms.user.api.UserCsvReader;
import no.hvl.dat250.lms.user.api.dto.CreateUserRequest;
import no.hvl.dat250.lms.user.api.dto.UpdateAccessRequest;
import no.hvl.dat250.lms.user.api.dto.UserImportResponse;
import no.hvl.dat250.lms.user.api.dto.UserPageResponse;
import no.hvl.dat250.lms.user.api.dto.UserResponse;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Administrator operations. {@code @IsAdmin} lets only callers with ADMIN access in, as
 * stored in the database at the time of the request.
 */
@RestController
@IsAdmin
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var user = users.createUser(request.firstName(), request.lastName(), request.email());
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.getUserId()))
                .body(UserResponse.from(user));
    }

    /**
     * Creates profiles for every user in an uploaded CSV file (field name "file"); see
     * {@link UserCsvReader} for the format. Answers 200 with a report per row, also when
     * some rows failed; 400 only if the file cannot be read as a user list at all.
     */
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserImportResponse importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        try (var in = file.getInputStream()) {
            return users.importUsers(UserCsvReader.read(in));
        }
    }

    @GetMapping
    public UserPageResponse listUsers(@RequestParam(defaultValue = "") String search,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return UserPageResponse.from(users.listUsers(search, page, size));
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return UserResponse.from(users.getUser(userId));
    }

    @PatchMapping("/{userId}/access")
    public UserResponse updateAccess(@AuthenticationPrincipal AuthenticatedUser admin,
                                     @PathVariable Long userId,
                                     @Valid @RequestBody UpdateAccessRequest request) {
        return UserResponse.from(users.updateAccess(admin.userId(), userId, request.access()));
    }
}
