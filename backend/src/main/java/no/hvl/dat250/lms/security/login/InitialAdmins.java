package no.hvl.dat250.lms.security.login;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.hvl.dat250.lms.user.domain.Access;

/**
 * Bootstrap for the first administrator(s).
 *
 * <p>Every new local profile starts as STANDARD, and only an ADMIN may change access, so
 * without this there would be no way to get the first ADMIN. The emails in
 * {@code lms.security.initial-admin-emails} get ADMIN when their local profile is
 * <em>created</em> at their first login. Existing profiles are never changed: after the
 * bootstrap, access is managed in the application, and an admin who was demoted there
 * stays demoted.
 *
 * <p>The email must be verified in Keycloak (checked by {@link AuthService} before this
 * is asked), so only someone who controls a Keycloak account with that address can
 * become the initial admin. Self-registration is disabled in the realm.
 */
@Component
public class InitialAdmins {
    private final Set<String> emails;

    public InitialAdmins(@Value("${lms.security.initial-admin-emails:}") List<String> emails) {
        this.emails = emails.stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** The access a profile for this verified email gets when it is created. */
    public Access initialAccessFor(String email) {
        return email != null && emails.contains(email.toLowerCase(Locale.ROOT))
                ? Access.ADMIN
                : Access.STANDARD;
    }
}
