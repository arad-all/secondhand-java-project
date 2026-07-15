package backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request, before Spring Security's authorization checks.
 * Reads {@code Authorization: Bearer <token>}, verifies it via
 * {@link JwtService}, and — if valid — places an
 * {@link AuthenticatedUser} into the {@code SecurityContext} so the rest
 * of the chain (and eventually controllers, via
 * {@code @AuthenticationPrincipal}) knows who's calling.
 * <p>
 * Per the plan's request-flow: on success this builds the
 * {@code Authentication} and stores it; on failure or absence, the
 * request simply proceeds unauthenticated. This filter never itself
 * rejects a request — that's {@link SecurityConfig}'s job (via
 * {@code authorizeHttpRequests} and the configured
 * {@code AuthenticationEntryPoint}), once it sees there's no
 * {@code Authentication} for a protected path.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Long userId = jwtService.extractUserId(token);
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);

                AuthenticatedUser principal = new AuthenticatedUser(userId, username, role);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid signature, expired, tampered, or a malformed subject/claim.
                // Don't throw from here — just leave the SecurityContext empty.
                // Public endpoints still work with no Authentication present;
                // protected endpoints get rejected with 401 further down the
                // chain by Spring Security itself (see SecurityConfig's
                // authenticationEntryPoint).
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
