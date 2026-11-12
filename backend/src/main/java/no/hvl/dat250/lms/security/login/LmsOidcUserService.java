package no.hvl.dat250.lms.security.login;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Runs once per login, right after the backend has exchanged the authorization code for
 * tokens and validated the id_token (signature, issuer, audience, expiry).
 *
 * <p>The verified email in the id_token identifies the user: the local {@code User} is
 * found by that email, or created on the first login. An unverified or missing email
 * makes the login fail, so no session is created.
 */
@Service
public class LmsOidcUserService extends OidcUserService {
    private final AuthService auth;

    public LmsOidcUserService(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        // No user-info-uri is configured, so this builds the user from the id_token alone.
        OidcUser identity = super.loadUser(request);
        try {
            auth.provisionUser(identity.getIdToken());
        } catch (OAuth2AuthenticationException ex) {
            // Keycloak has logged the user in; keep the id_token so the failure handler
            // can end that Keycloak session (see LoginFailureHandler).
            throw new LoginRejectedException(ex, identity.getIdToken());
        }
        return identity;
    }
}
