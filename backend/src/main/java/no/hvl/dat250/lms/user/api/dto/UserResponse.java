package no.hvl.dat250.lms.user.api.dto;

import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;

/**
 * User information returned by the API, excluding the password hash.
 *
 * @param userId the user's generated primary key
 * @param email the user's email address
 * @param firstName the user's first name
 * @param lastName the user's last name
 * @param access the user's global access level
 * @param mustChangePassword whether auth must require a password change
 */
public record UserResponse(
        Long userId,
        String email,
        String firstName,
        String lastName,
        Access access,
        boolean mustChangePassword
) {
    /**
     * Copies the public profile fields from a user into a response.
     *
     * @param user the user to convert
     * @return the user's profile response
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccess(),
                user.isMustChangePassword()
        );
    }
}
