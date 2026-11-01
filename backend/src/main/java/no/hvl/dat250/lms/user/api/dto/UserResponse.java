package no.hvl.dat250.lms.user.api.dto;

import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;

/**
 * User information returned by the API, excluding the password hash.
 *
 * @param id the user's ID
 * @param username the user's username
 * @param email the user's email address
 * @param access the user's global access level
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        Access access
) {
    /**
     * Copies the public profile fields from a user into a response.
     *
     * @param user the user to convert
     * @return the user's profile response
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAccess()
        );
    }
}
