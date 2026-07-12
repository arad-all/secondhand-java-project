package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown on login when the username doesn't exist or the password doesn't
 * match. Deliberately used for BOTH cases with the same generic message —
 * telling a caller "that username doesn't exist" vs "that password is
 * wrong" would let an attacker enumerate valid usernames one guess at a
 * time. Maps to HTTP 401.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }
}
