package no.hvl.dat250.lms.security.login;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/**
 * The Keycloak login succeeded, but the LMS refused the user (not invited, unverified
 * email, invalid profile). Keeps the id_token so that {@link LoginFailureHandler} can end
 * the Keycloak session; otherwise the next login would silently reuse it and fail again.
 */
public class LoginRejectedException extends OAuth2AuthenticationException {
    private final OidcIdToken idToken;

    public LoginRejectedException(OAuth2AuthenticationException cause, OidcIdToken idToken) {
        super(cause.getError(), cause);
        this.idToken = idToken;
    }

    public OidcIdToken getIdToken() {
        return idToken;
    }
}
