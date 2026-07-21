package backend.security;

import backend.exception.ErrorResponse;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Optional;

/**
 * Runs once per request, before Spring Security's authorization checks.
 * Reads {@code Authorization: Bearer <token>}, verifies it via
 * {@link JwtService}, and — if valid — places an
 * {@link AuthenticatedUser} into the {@code SecurityContext} so the rest
 * of the chain (and eventually controllers, via
 * {@code @AuthenticationPrincipal}) knows who's calling.
 * <p>
 * A verified signature only proves the token was issued by us at some
 * point in the past — it says nothing about whether the account it names
 * is still in good standing right now. Since tokens live for
 * {@code jwt.expiration-ms} (24h by default) and nothing here is
 * session-based, an admin blocking a user via
 * {@code AdminController.blockUser} would otherwise have no effect until
 * that user's existing token expired on its own. So every request bearing
 * a token is checked against the account's *current* {@link AccountStatus}
 * in the database: a {@code BLOCKED} account's token is rejected outright,
 * with the same 403 {@code AccountBlockedException} would produce at
 * login, rather than being allowed to reach even the endpoints that are
 * normally public (browsing, etc.) — a blocked account should not be able
 * to do anything through the API, not just log in again.
 * <p>
 * Per the plan's request-flow: on success this builds the
 * {@code Authentication} and stores it; on failure or absence, the
 * request simply proceeds unauthenticated. This filter never itself
 * rejects a request for a missing/invalid token — that's
 * {@link SecurityConfig}'s job (via {@code authorizeHttpRequests} and the
 * configured {@code AuthenticationEntryPoint}), once it sees there's no
 * {@code Authentication} for a protected path. A blocked account is the
 * one case this filter does reject directly, since by definition that
 * account must never be treated as authenticated again.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
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

                Optional<User> currentUser = userRepository.findById(userId);
                if (currentUser.isEmpty()) {
                    // Account no longer exists (e.g. deleted after the token
                    // was issued) - treat exactly like an invalid token.
                    SecurityContextHolder.clearContext();
                } else if (currentUser.get().getStatus() == AccountStatus.BLOCKED) {
                    SecurityContextHolder.clearContext();
                    writeBlockedResponse(response);
                    return;
                } else {
                    AuthenticatedUser principal = new AuthenticatedUser(userId, username, role);
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
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

    /**
     * Same {message, status} shape {@code GlobalExceptionHandler} uses for
     * every other error, and the same message {@code AccountBlockedException}
     * carries at login — written directly rather than thrown, since a
     * rejection here happens before the request ever reaches a controller
     * or {@code GlobalExceptionHandler}.
     */
    private void writeBlockedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                new ErrorResponse("This account has been blocked.", HttpStatus.FORBIDDEN.value())));
    }
}
