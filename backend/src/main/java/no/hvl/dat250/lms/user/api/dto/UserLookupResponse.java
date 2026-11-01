package no.hvl.dat250.lms.user.api.dto;

import no.hvl.dat250.lms.user.domain.User;

/** Only the identity/contact fields needed to select an existing account for enrollment. */
public record UserLookupResponse(Long userId, String firstName, String lastName, String email) {
    public static UserLookupResponse from(User user) {
        return new UserLookupResponse(user.getUserId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
