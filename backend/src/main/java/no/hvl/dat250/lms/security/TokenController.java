package no.hvl.dat250.lms.security;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * GET /token: exchanges the login session (cookie) for an API token.
 *
 * <p>Only reachable with a session from the Keycloak login (see the login filter chain),
 * so the principal is the {@link OidcUser} built from the id_token.
 */
@RestController
public class TokenController {
    private final AuthService auth;
    private final TokenService tokens;

    public TokenController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @GetMapping("/token")
    public ResponseEntity<TokenResponse> token(@AuthenticationPrincipal OidcUser identity) {
        if (identity == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        TokenResponse token;
        try {
            token = tokens.issue(auth.findUser(identity.getIdToken()));
        } catch (OAuth2AuthenticationException ex) {
            // The local profile was removed after login: the user must log in again.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        // The token is a credential: no browser or proxy may store the response.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(token);
    }
}
