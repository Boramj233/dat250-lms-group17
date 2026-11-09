package no.hvl.dat250.lms.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Part of POST /logout in the login chain: revokes all API tokens of the logged-in user.
 *
 * <p>Runs before Spring invalidates the session and before the redirect to Keycloak's
 * logout, while the session's {@link Authentication} is still available. The user is
 * identified the same way as at GET /token: by the verified email in the id_token.
 */
@Component
public class TokenRevokingLogoutHandler implements LogoutHandler {
    private final AuthService auth;
    private final TokenService tokens;

    public TokenRevokingLogoutHandler(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser identity)) {
            return; // No login session: there are no tokens to revoke.
        }
        try {
            tokens.revokeAllFor(auth.findUser(identity.getIdToken()).getUserId());
        } catch (OAuth2AuthenticationException ex) {
            // The local profile no longer exists, so its tokens are already unusable.
        }
    }
}
