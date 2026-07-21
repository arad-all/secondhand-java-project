package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an uploaded file (currently: advertisement images) fails
 * validation — missing/empty, an unsupported content type, or a request
 * that would exceed the per-advertisement image limit. Maps to HTTP 400:
 * the request is well-formed as an HTTP request, but the file(s) it
 * carries aren't acceptable.
 */
public class InvalidFileException extends ApiException {

    public InvalidFileException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
