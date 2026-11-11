package no.hvl.dat250.lms.security.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * Where the browser goes when a login fails. The SPA shows "Login failed" for
 * {@code ?login_error} and for {@code ?state=login_error}.
 *
 * <ul>
 *   <li>The LMS refused a user that Keycloak had logged in ({@link LoginRejectedException}):
 *       end the Keycloak session first. Otherwise the user is stuck: the next click on
 *       "Log in" would reuse the Keycloak session without showing the login form, and the
 *       LMS would refuse the same user again.</li>
 *   <li>Any other failure (e.g. the user cancelled at Keycloak): back to {@code /?login_error}.</li>
 * </ul>
 */
public class LoginFailureHandler implements AuthenticationFailureHandler {
    /** The marker the SPA looks for, as query parameter or as Keycloak's {@code state}. */
    public static final String LOGIN_ERROR = "login_error";

    private final String issuerUri;
    private final String clientId;
    private final AuthenticationFailureHandler toSpa =
            new SimpleUrlAuthenticationFailureHandler("/?" + LOGIN_ERROR);

    public LoginFailureHandler(String issuerUri, String clientId) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, jakarta.servlet.ServletException {
        if (exception instanceof LoginRejectedException rejected && rejected.getIdToken() != null) {
            response.sendRedirect(KeycloakLogout.uri(request, issuerUri, clientId,
                    rejected.getIdToken().getTokenValue(), LOGIN_ERROR));
            return;
        }
        toSpa.onAuthenticationFailure(request, response, exception);
    }
}
