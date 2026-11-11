package no.hvl.dat250.lms.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import no.hvl.dat250.lms.security.login.LoginFailureHandler;
import no.hvl.dat250.lms.security.login.LoginRejectedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/** Where a failed login sends the browser. */
class LoginFailureHandlerTest {
    private static final String ISSUER = "http://localhost:8081/realms/lms";

    private final LoginFailureHandler handler = new LoginFailureHandler(ISSUER, "spring");

    @Test
    void refusedUserIsLoggedOutOfKeycloakSoTheNextLoginShowsTheForm() throws Exception {
        var idToken = OidcIdToken.withTokenValue("id-token-value")
                .issuer(ISSUER).subject("keycloak-id")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .build();
        var refused = new LoginRejectedException(
                new OAuth2AuthenticationException(new OAuth2Error("not_invited")), idToken);
        var response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, refused);

        String location = response.getRedirectedUrl();
        assertThat(location, startsWith(ISSUER + "/protocol/openid-connect/logout?"));
        assertThat(location, containsString("client_id=spring"));
        assertThat(location, containsString("post_logout_redirect_uri=http://localhost/"));
        assertThat(location, containsString("id_token_hint=id-token-value"));
        assertThat(location, containsString("state=login_error"));
    }

    @Test
    void otherFailuresGoStraightBackToTheSpa() throws Exception {
        var response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertEquals("/?login_error", response.getRedirectedUrl());
    }
}
