package backend.security;

import backend.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the JSON Web Tokens used to authenticate requests
 * after login. A JWT is just a signed, tamper-evident bundle of claims
 * (here: which user, their username, their role) that the server can
 * verify without needing to look anything up in the database or keep any
 * server-side session state — that's what makes the API "stateless"
 * (see {@code SecurityConfig}'s {@code SessionCreationPolicy.STATELESS}).
 * <p>
 * Two things use this class: {@code AuthService.login(...)} calls
 * {@link #generateToken(User)} to hand back a token; the JWT filter (added
 * in Part 3) will call {@link #parseClaims(String)} on every subsequent
 * request to figure out who's calling, without trusting anything the
 * client merely claims about itself.
 */
@Component
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMillis;

    /**
     * {@code jwt.secret} and {@code jwt.expiration-ms} are read from
     * application.properties/yml (with fallback defaults below) rather
     * than hardcoded, so the signing key isn't committed to source control
     * long-term and the token lifetime is easy to tune per environment.
     * The secret must be at least 256 bits (32 characters) for the HS256
     * algorithm used here — {@link Keys#hmacShaKeyFor} enforces this and
     * throws if it's too short.
     */
    public JwtService(
            @Value("${jwt.secret:CHANGE_ME_this_is_a_dev_only_placeholder_secret_key_32b}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * Builds a signed token for the given user. The subject is the user's
     * id (as a string, since JWT subjects are conventionally strings) —
     * everything downstream should resolve "current user" from this token,
     * never from a client-supplied field, which is exactly the rule the
     * plan's DTOs (e.g. {@code CreateAdvertisementRequest} having no
     * {@code ownerId}) were designed around.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the token's signature and expiry, then returns its claims.
     * Throws an unchecked {@link JwtException} (or the more specific
     * {@link ExpiredJwtException}) if the token is invalid, tampered with,
     * or expired — the JWT filter (Part 3) is responsible for catching
     * that and turning it into a 401, not this class.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Convenience accessor: pulls the user id out of a token's subject claim. */
    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /** Convenience accessor: pulls the {@code role} claim out of a token. */
    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }
}
