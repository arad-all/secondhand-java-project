package backend.security;

/**
 * The Spring Security principal placed into the {@code SecurityContext} by
 * {@link JwtAuthenticationFilter} once a request's JWT has been verified.
 * <p>
 * Carries exactly what every downstream controller needs to resolve "who
 * is calling" without a second database lookup per request — the JWT
 * itself is the source of truth for these three fields (see
 * {@link JwtService#generateToken}). Controllers read this via
 * {@code @AuthenticationPrincipal AuthenticatedUser user} and pass
 * {@code user.userId()} / {@code user.role()} into service methods as
 * plain parameters — services never reach into the
 * {@code SecurityContext} themselves.
 */
public record AuthenticatedUser(Long userId, String username, String role) {
}
