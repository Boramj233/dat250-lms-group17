package no.hvl.dat250.lms.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class LmsOidcUserService extends OidcUserService {
    private final AuthService auth;
    private final String issuer;

    public LmsOidcUserService(AuthService auth,
            @Value("${KEYCLOAK_PUBLIC_URL:http://localhost:8081}/realms/lms") String issuer) {
        this.auth = auth;
        this.issuer = issuer;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        // Endpoints use separate public/internal hosts, so validate the public issuer explicitly.
        if (!issuer.equals(request.getIdToken().getClaimAsString("iss"))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_issuer"));
        }
        OidcUser identity = super.loadUser(request);
        try {
            auth.provisionUser(identity);
        } catch (jakarta.validation.ConstraintViolationException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_profile"), ex);
        }
        return identity;
    }
}
