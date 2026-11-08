package no.hvl.dat250.lms.security;

import java.util.List;
import no.hvl.dat250.lms.user.domain.AuthenticatedUser;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns a validated Keycloak JWT into the application's own authentication.
 *
 * <p>Runs on every API request, after the signature and claims have been checked.
 * The local profile is found (or created on first login) by verified email, and the
 * role comes from the database, so access changes take effect on the next request.
 * The principal is {@link AuthenticatedUser}, the same type controllers and services
 * already use.
 *
 * <p>Created in {@code SecurityConfig}, deliberately not a {@code @Component}: Spring MVC
 * would otherwise also register it as a general type converter.
 */
public class LmsJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final AuthService auth;

    public LmsJwtAuthenticationConverter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = auth.provisionUser(jwt);
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getAccess().name());
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getUserId()), jwt, List.of(authority));
    }
}
