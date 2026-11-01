package no.hvl.dat250.lms.user.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import no.hvl.dat250.lms.user.api.dto.ChangePasswordRequest;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/** Credential verification for the password endpoint; reusable by future auth flows. */
@Service
public class UserPasswordService {
    private final UserService users;
    private final PasswordEncoder passwords;
    private final Validator validator;

    public UserPasswordService(UserService users, PasswordEncoder passwords, Validator validator) {
        this.users = users;
        this.passwords = passwords;
        this.validator = validator;
    }

    public void changeOwnPassword(ChangePasswordRequest request) {
        User user = users.getCurrentUser();
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        // The default encoder writes bcrypt hashes, whose input limit is 72 bytes.
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("New password exceeds 72 UTF-8 bytes");
        }
        String currentHash = user.getPassword();
        if (!passwords.matches(request.currentPassword(), currentHash)) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwords.matches(request.newPassword(), currentHash)) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }
        users.changePassword(currentHash, passwords.encode(request.newPassword()));
    }
}
