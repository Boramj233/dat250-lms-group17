package no.hvl.dat250.lms.security;

import java.util.List;

import no.hvl.dat250.lms.security.token.TokenService;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;

/** API credentials for MockMvc tests. */
public final class TestTokens {
    private TestTokens() {}

    /**
     * Authorization header with a real token from {@link TokenService}, exactly as the SPA
     * gets it from GET /token. The full API chain runs: introspection, database lookup,
     * {@code @PreAuthorize} and the services.
     */
    public static String bearer(TokenService tokens, User user) {
        return "Bearer " + tokens.issue(user).accessToken();
    }

    /**
     * For controller slice tests ({@link WithApiSecurity}), where TokenService is a mock:
     * the request is authenticated as if introspection had returned this principal. The
     * filter chain, CSRF/session settings and method security still run.
     */
    public static RequestPostProcessor apiUser(long userId, Access access) {
        var authority = new SimpleGrantedAuthority("ROLE_" + access.name());
        return opaqueToken().principal(new AuthenticatedUser(userId, List.of(authority)));
    }

    /** A STANDARD user, the default caller in controller slice tests. */
    public static RequestPostProcessor apiUser() {
        return apiUser(1L, Access.STANDARD);
    }
}
