package no.hvl.dat250.lms.security.token;

import java.util.List;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.login.AuthService;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Component;

/**
 * Turns a validated Keycloak access token into the API caller: an {@link AuthenticatedUser}
 * with {@code ROLE_<access>}.
 *
 * <p>Called by the API filter chain for every request, after the JWT's signature, expiry,
 * issuer and audience have been checked. The verified email identifies the local user,
 * exactly as at the web login; the access level is read from the database every time, so
 * a changed access level or a deleted user applies to the very next request, even though
 * the JWT itself is still valid.
 *
 * <p>The user must already have a local profile (from a web login, or created by an
 * admin). Otherwise {@link AuthService#findUser} throws an
 * {@link OAuth2AuthenticationException}, which the API chain answers with 401.
 *
 * <p>Deliberately not a Spring {@code Converter} bean: Spring MVC would register it as a
 * request parameter converter, and {@code @WebMvcTest} slices would pick it up. The API
 * chain uses it as {@code jwtToUser::convert}.
 */
@Component
public class LmsJwtAuthenticationConverter {
    private final AuthService auth;

    public LmsJwtAuthenticationConverter(AuthService auth) {
        this.auth = auth;
    }

    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = auth.findUser(jwt);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getAccess().name()));
        var principal = new AuthenticatedUser(user.getUserId(), authorities);
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt());
        return new BearerTokenAuthentication(principal, token, authorities);
    }
}
