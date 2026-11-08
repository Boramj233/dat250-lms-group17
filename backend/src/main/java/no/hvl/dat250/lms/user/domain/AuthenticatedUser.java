package no.hvl.dat250.lms.user.domain;

import java.security.Principal;
import java.util.Objects;

/**
 * Identity of the caller for the current request: only the local user ID.
 *
 * <p>Deliberately not the {@link User} entity. Services load the user through the
 * repository layer when they need it, so profile data and access are always read
 * fresh inside the service's own transaction.
 */
public record AuthenticatedUser(Long userId) implements Principal {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
