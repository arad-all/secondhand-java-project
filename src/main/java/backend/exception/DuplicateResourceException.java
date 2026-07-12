package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would violate a uniqueness rule (e.g. a
 * username, phone number, or email that's already taken). Maps to HTTP
 * 409 Conflict, distinct from a validation failure (400) because the
 * request is well-formed — it just collides with existing data.
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
