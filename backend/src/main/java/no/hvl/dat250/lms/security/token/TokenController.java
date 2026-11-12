package no.hvl.dat250.lms.security.token;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import no.hvl.dat250.lms.security.login.AuthService;

/**
 * GET /token: hands the SPA the Keycloak access token (a signed JWT) of its login session.
 *
 * <p>Only reachable with a session from the Keycloak login (see the login filter chain),
 * so the principal is the {@link OidcUser} built from the id_token. The access and refresh
 * tokens from the code exchange are stored in that session; when the access token is
 * about to expire, the {@link OAuth2AuthorizedClientManager} gets a new one from Keycloak
 * with the refresh token before it is returned.
 *
 * <p>Third-party clients do not use this endpoint: they get their access token from
 * Keycloak directly (client {@code lms-cli}).
 */
@RestController
public class TokenController {
    private static final String REGISTRATION_ID = "keycloak";

    private final AuthService auth;
    private final OAuth2AuthorizedClientManager clients;

    public TokenController(AuthService auth, OAuth2AuthorizedClientManager clients) {
        this.auth = auth;
        this.clients = clients;
    }

    @GetMapping("/token")
    public ResponseEntity<TokenResponse> token(Authentication authentication,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser identity)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            // The local profile was removed after login: the user must log in again.
            auth.findUser(identity.getIdToken());
        } catch (OAuth2AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        OAuth2AuthorizedClient client;
        try {
            client = clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                    .principal(authentication)
                    .attribute(HttpServletRequest.class.getName(), request)
                    .attribute(HttpServletResponse.class.getName(), response)
                    .build());
        } catch (OAuth2AuthorizationException ex) {
            // No tokens in the session, or the refresh failed (Keycloak session ended).
            // Answer 401 instead of redirecting: the SPA then shows its Login button.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        OAuth2AccessToken accessToken = client.getAccessToken();
        // The token is a credential: no browser or proxy may store the response.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(new TokenResponse(accessToken.getTokenValue(), "Bearer", accessToken.getExpiresAt()));
    }
}
