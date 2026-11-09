package no.hvl.dat250.lms.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * For {@code @WebMvcTest} controller tests: runs them with the application's real
 * {@link SecurityConfig} (both filter chains and method security) instead of turning the
 * security filters off. The token and login services are mocked; authenticate requests
 * with {@link TestTokens#apiUser()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@MockitoBean(types = {TokenService.class, LmsOidcUserService.class})
public @interface WithApiSecurity {}
