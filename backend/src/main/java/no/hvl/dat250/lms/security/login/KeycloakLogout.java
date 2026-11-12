package no.hvl.dat250.lms.security.login;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the URL of Keycloak's end-session endpoint, which ends the Keycloak (SSO) session
 * and then sends the browser back to the SPA.
 */
public final class KeycloakLogout {
    private KeycloakLogout() {}

    /**
     * @param idTokenHint the user's id_token; without it Keycloak asks the user to confirm
     *                    the logout. May be null.
     * @param state       returned unchanged by Keycloak as {@code ?state=...} on the SPA's
     *                    address (Keycloak accepts only the registered post-logout URIs,
     *                    so the SPA URL itself cannot carry a query). May be null.
     */
    public static String uri(HttpServletRequest request, String issuerUri, String clientId,
                             String idTokenHint, String state) {
        String home = ServletUriComponentsBuilder.fromContextPath(request).path("/").toUriString();
        var uri = UriComponentsBuilder.fromUriString(issuerUri + "/protocol/openid-connect/logout")
                .queryParam("client_id", clientId)
                .queryParam("post_logout_redirect_uri", home);
        if (idTokenHint != null) {
            uri.queryParam("id_token_hint", idTokenHint);
        }
        if (state != null) {
            uri.queryParam("state", state);
        }
        return uri.encode().build().toUriString();
    }
}
