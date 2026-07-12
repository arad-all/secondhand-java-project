package backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration — Part 1 (foundations) version.
 * <p>
 * This is deliberately permissive for now: it exists so the app has a
 * {@link PasswordEncoder} bean available (needed by {@code AuthService} in
 * Part 2) and so adding {@code spring-boot-starter-security} as a
 * dependency doesn't lock every endpoint behind a login prompt before
 * we've actually built one.
 * <p>
 * <b>This will change in Part 3:</b> once the JWT filter exists, the
 * {@code authorizeHttpRequests} block below gets replaced with real rules
 * ({@code /api/auth/**} public, everything else authenticated,
 * {@code /api/admin/**} admin-only), and the JWT filter gets registered
 * into the chain via {@code .addFilterBefore(...)}. Until then, every
 * endpoint is intentionally open.
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt is the standard choice for password hashing: it's salted
     * automatically (two users with the same password get different
     * hashes) and its cost factor can be tuned up over time as hardware
     * gets faster, without changing any calling code.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends against a browser being tricked into
                // submitting a cookie-authenticated request. We're a stateless,
                // token-authenticated JSON API called from a JavaFX client, not
                // a cookie/session-based browser app, so CSRF doesn't apply here.
                .csrf(AbstractHttpConfigurer::disable)
                // TODO (Part 3): replace this with real per-path rules once the
                // JWT filter exists, e.g.:
                //   .authorizeHttpRequests(auth -> auth
                //       .requestMatchers("/api/auth/**").permitAll()
                //       .requestMatchers("/api/admin/**").hasRole("ADMIN")
                //       .anyRequest().authenticated())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
