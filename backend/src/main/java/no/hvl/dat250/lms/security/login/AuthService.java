package no.hvl.dat250.lms.security.login;

import jakarta.validation.ConstraintViolationException;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.stereotype.Service;

/**
 * Links a verified Keycloak identity to the local user profile.
 *
 * <p>The identity is a set of signed claims, in practice the id_token from the login
 * (see {@link LmsOidcUserService}). Only the verified email identifies the user.
 *
 * <p>Who may use the LMS is decided here, not in Keycloak. Anyone may register an account
 * in Keycloak, but with {@code lms.security.require-invitation=true} (the default) only
 * emails with a local profile get in: profiles an admin imported or created (the class
 * list), plus the configured initial admins ({@link InitialAdmins}).
 */
@Service
public class AuthService {
    private final UserService users;
    private final InitialAdmins initialAdmins;
    private final boolean requireInvitation;

    public AuthService(UserService users, InitialAdmins initialAdmins,
                       @Value("${lms.security.require-invitation:true}") boolean requireInvitation) {
        this.users = users;
        this.initialAdmins = initialAdmins;
        this.requireInvitation = requireInvitation;
    }

    /**
     * Finds the local profile for the token's verified email. Without one, the login is
     * refused when invitations are required; otherwise (or for a configured initial
     * admin) a profile is created: STANDARD, or ADMIN for an initial admin. Existing
     * access is never changed here.
     *
     * @throws OAuth2AuthenticationException (login fails) if the email is missing or
     *         not verified, the user is not invited, or the profile data from Keycloak
     *         is invalid
     */
    public User provisionUser(ClaimAccessor token) {
        String email = verifiedEmail(token);
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        // The initial access only applies if this login creates the profile.
        Access initialAccess = initialAdmins.initialAccessFor(email);
        if (requireInvitation && initialAccess != Access.ADMIN) {
            throw new OAuth2AuthenticationException(new OAuth2Error("not_invited",
                    "This account has no LMS profile; ask an administrator to add it", null));
        }
        String firstName = token.getClaimAsString("given_name");
        String lastName = token.getClaimAsString("family_name");
        // Some providers supply only the full name. Local profiles require both fields.
        if (firstName == null || firstName.isBlank()) {
            firstName = token.getClaimAsString("name");
        }
        if (firstName == null || firstName.isBlank()) {
            firstName = email;
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = firstName;
        }
        try {
            return users.findOrCreateOidcUser(email, firstName, lastName, initialAccess);
        } catch (DataIntegrityViolationException ex) {
            // Two first requests for the same new user raced; the other one created it.
            return users.findByEmail(email).orElseThrow(() -> ex);
        } catch (ConstraintViolationException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_profile"), ex);
        }
    }

    /**
     * Finds the existing local profile for a logged-in identity, without creating one.
     *
     * @throws OAuth2AuthenticationException if the email is not verified or no profile exists
     */
    public User findUser(ClaimAccessor token) {
        return users.findByEmail(verifiedEmail(token))
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("unknown_user")));
    }

    private String verifiedEmail(ClaimAccessor token) {
        // Trust only the signed token, never request parameters or an arbitrary header.
        String email = token.getClaimAsString("email");
        if (!Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"))
                || email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("verified_email_required"));
        }
        return email;
    }
}
