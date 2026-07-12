package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity (by id, username, etc.) doesn't exist.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
