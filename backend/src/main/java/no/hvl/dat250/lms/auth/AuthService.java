package no.hvl.dat250.lms.auth;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserService users;

    public AuthService(UserService users) {
        this.users = users;
    }

    public User provisionUser(OidcUser identity) {
        String email = verifiedEmail(identity);
        String firstName = identity.getGivenName();
        String lastName = identity.getFamilyName();
        // Some providers supply only the full name. Local profiles require both fields.
        if (firstName == null || firstName.isBlank()) {
            firstName = identity.getFullName();
        }
        if (firstName == null || firstName.isBlank()) {
            firstName = email;
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = firstName;
        }
        return users.findOrCreateOidcUser(email, firstName, lastName);
    }

    public User findUser(OidcUser identity) {
        return users.findByEmail(verifiedEmail(identity))
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_user")));
    }

    private String verifiedEmail(OidcUser identity) {
        // Trust the signed ID token, not request parameters or an arbitrary email header.
        String email = identity.getIdToken().getEmail();
        if (!Boolean.TRUE.equals(identity.getIdToken().getEmailVerified())
                || email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("verified_email_required"));
        }
        return email;
    }
}
