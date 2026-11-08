package no.hvl.dat250.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import no.hvl.dat250.lms.auth.ApiTokenService;
import no.hvl.dat250.lms.auth.LmsOidcUserService;
import no.hvl.dat250.lms.user.domain.User;
import java.time.Clock;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, ApiTokenService tokens) throws Exception {
        http.securityMatcher("/api/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(cache -> cache.disable())
            // API authentication is an explicit header; cookies cannot authenticate these requests.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(server -> server.opaqueToken(opaque -> opaque
                .introspector(tokens)
                .authenticationConverter((token, principal) -> {
                    User user = principal.getAttribute("user");
                    return UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
                })));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain loginSecurity(HttpSecurity http, LmsOidcUserService oidcUsers) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/token").authenticated()
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/error",
                        "/login", "/login/**", "/oauth2/**").permitAll()
                .anyRequest().denyAll()
            )
            .requestCache(cache -> cache.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUsers))
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
            );

        return http.build();
    }
}
