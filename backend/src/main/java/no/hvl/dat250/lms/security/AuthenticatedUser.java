package no.hvl.dat250.lms.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import no.hvl.dat250.lms.security.token.LmsJwtAuthenticationConverter;

/**
 * Identity of the caller for the current API request: the local user ID and the
 * authorities (ROLE_&lt;access&gt;) read from the database when the token was checked.
 *
 * <p>This is the principal that {@link LmsJwtAuthenticationConverter} builds from the
 * Keycloak access token, so
 * controllers get it with {@code @AuthenticationPrincipal AuthenticatedUser}.
 *
 * <p>Deliberately not the {@code User} entity. Services load the user through the
 * repository layer when they need it, so profile data and access are always read
 * fresh inside the service's own transaction.
 */
public record AuthenticatedUser(Long userId, Collection<? extends GrantedAuthority> authorities)
        implements OAuth2AuthenticatedPrincipal {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
        authorities = List.copyOf(authorities);
    }

    /** A principal without authorities, for code that only needs the user ID. */
    public AuthenticatedUser(Long userId) {
        this(userId, List.of());
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("user_id", userId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
