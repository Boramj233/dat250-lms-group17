package no.hvl.dat250.lms.auth;

import no.hvl.dat250.lms.auth.dto.TokenResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
    private final AuthService auth;
    private final ApiTokenService tokens;

    public AuthController(AuthService auth, ApiTokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @GetMapping("/token")
    public ResponseEntity<TokenResponse> token(@AuthenticationPrincipal OidcUser identity) {
        if (identity == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache").body(tokens.issue(auth.findUser(identity)));
        } catch (org.springframework.security.oauth2.core.OAuth2AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
