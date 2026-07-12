package backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every business-rule exception thrown by the service layer.
 * Carries the HTTP status that should be sent back for it, so the global
 * exception handler ({@link backend.exception.GlobalExceptionHandler}) can
 * translate any subclass into the right response without needing an
 * if/else chain of instanceof checks.
 * <p>
 * Services should throw one of the specific subclasses below rather than
 * this class directly, so the intent of the failure stays obvious at the
 * call site (e.g. {@code throw new ResourceNotFoundException(...)} reads
 * better than a generic {@code throw new ApiException(...)}).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
