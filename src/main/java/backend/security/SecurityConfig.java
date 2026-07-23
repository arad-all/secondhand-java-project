package backend.security;

import backend.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Security configuration — sets up stateless JWT authentication,
 * role-based access, and public-read exceptions for browsing endpoints.
 * <p>
 * Baseline rule: {@code /api/auth/**} is public (you can't be
 * authenticated before you've logged in), {@code /api/admin/**} requires
 * the {@code ADMIN} role, and everything else requires *some*
 * authenticated user. Sessions are stateless — every request must carry
 * its own JWT, since nothing about "who's logged in" is kept server-side
 * between requests.
 * <p>
 * On top of that baseline, a handful of specific GET routes are carved
 * out as public because the services behind them are public-read by
 * design (plan §"Reference data" / §"Advertisements core"): browsing and
 * viewing ads, and listing categories/cities, need no current user.
 * {@code /api/advertisements/my} and
 * {@code /api/advertisements/purchased} are deliberately matched *before*
 * the broader {@code /api/advertisements/**} public rule so that the more
 * specific "authenticated" requirement wins for those paths — Spring
 * Security's {@code authorizeHttpRequests} evaluates matchers in
 * declaration order and stops at the first match.
 * <p>
 * {@code @EnableMethodSecurity} activates {@code @PreAuthorize}, used on
 * the category/city {@code create} endpoints: those live at plain
 * {@code /api/categories} / {@code /api/cities} paths (not under
 * {@code /api/admin/**}), so URL-pattern-based role matching can't cover
 * them — a method-level check is the simplest way to keep them
 * admin-only without reshaping the URL scheme.
 * <p>
 * {@link JwtAuthenticationFilter} is registered ahead of Spring Security's
 * own {@link UsernamePasswordAuthenticationFilter} so that, by the time
 * {@code authorizeHttpRequests} runs its checks, the {@code SecurityContext}
 * already reflects whatever a valid token proved.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

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
                // No server-side session — every request proves its own identity
                // via the JWT in the Authorization header.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Neither of Spring Security's default login mechanisms apply to
                // a token-based JSON API — without disabling these, an
                // unauthenticated request could get redirected to a login page
                // or challenged for HTTP Basic instead of getting a plain 401.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // More specific than the /api/advertisements/** rule below,
                        // and declared first so it takes precedence: owner and purchase
                        // history always need a real caller, even though these paths
                        // would otherwise be swallowed by the public wildcard.
                        .requestMatchers(HttpMethod.GET,
                                "/api/advertisements/my",
                                "/api/advertisements/purchased").authenticated()
                        // Public browsing: search/list and single-ad lookups.
                        // AdvertisementService.getById still hides non-ACTIVE ads
                        // from non-owners/non-admins itself, so making this route
                        // public is safe — it doesn't widen what's actually visible.
                        .requestMatchers(HttpMethod.GET, "/api/advertisements", "/api/advertisements/**").permitAll()
                        // Reference data: no auth dependency for reads, by design.
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories", "/api/categories/**",
                                "/api/cities", "/api/cities/**").permitAll()
                        // A seller's reputation is meant to be visible before you'd
                        // ever log in to buy from them, same reasoning as ad browsing.
                        .requestMatchers(HttpMethod.GET, "/api/users/*/ratings").permitAll()
                        // Likewise, a seller's public profile (username/name/phone) —
                        // e.g. opened from an ad's detail view — needs no login either.
                        .requestMatchers(HttpMethod.GET, "/api/users/by-username/*").permitAll()
                        .anyRequest().authenticated())
                // Turns Spring Security's own rejections (no/invalid token on a
                // protected path -> 401; wrong role -> 403) into the same
                // {"message": ..., "status": ...} JSON shape GlobalExceptionHandler
                // uses for every other error, instead of Spring's default
                // (empty-body or HTML) response.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(new ErrorResponse(message, status.value())));
    }
}
