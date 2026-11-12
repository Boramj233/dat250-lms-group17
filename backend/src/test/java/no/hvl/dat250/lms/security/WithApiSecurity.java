package no.hvl.dat250.lms.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import no.hvl.dat250.lms.security.login.LmsOidcUserService;
import no.hvl.dat250.lms.security.token.LmsJwtAuthenticationConverter;

/**
 * For {@code @WebMvcTest} controller tests: runs them with the application's real
 * {@link SecurityConfig} (both filter chains and method security) instead of turning the
 * security filters off. The JWT-to-user conversion and the login service are mocked;
 * authenticate requests with {@link TestTokens#apiUser()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@MockitoBean(types = {LmsJwtAuthenticationConverter.class, LmsOidcUserService.class})
public @interface WithApiSecurity {}
