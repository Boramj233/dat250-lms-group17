package no.hvl.dat250.lms.security;

import java.time.Instant;
import java.util.Set;

import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.security.login.LmsOidcUserService;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Step 4 of the login: the id_token decides who the user is. */
class LmsOidcUserServiceTest {
    private final UserService users = mock(UserService.class);
    private final LmsOidcUserService service = new LmsOidcUserService(new AuthService(users));

    private static OidcUserRequest request(boolean emailVerified) {
        var registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("spring").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("http://localhost:8081/realms/lms/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8081/realms/lms/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8081/realms/lms/protocol/openid-connect/certs")
                .userNameAttributeName("sub")
                .build();
        Instant now = Instant.now();
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token",
                now, now.plusSeconds(300), Set.of("openid", "profile", "email"));
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://localhost:8081/realms/lms").subject("keycloak-id").audience(Set.of("spring"))
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("email", "teacher@example.com").claim("email_verified", emailVerified)
                .claim("given_name", "Demo").claim("family_name", "Teacher").claim("name", "Demo Teacher")
                .build();
        return new OidcUserRequest(registration, accessToken, idToken);
    }

    @Test
    void firstLoginCreatesTheLocalUserFromTheIdToken() {
        when(users.findOrCreateOidcUser("teacher@example.com", "Demo", "Teacher"))
                .thenReturn(new User("teacher@example.com", "Demo", "Teacher"));
        var identity = service.loadUser(request(true));
        assertEquals("teacher@example.com", identity.getEmail());
        verify(users).findOrCreateOidcUser("teacher@example.com", "Demo", "Teacher");
    }

    @Test
    void unverifiedEmailMakesTheLoginFail() {
        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request(false)));
        verifyNoInteractions(users);
    }
}
