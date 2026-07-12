package backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Catches every exception thrown out of a controller method (whether it
 * escaped a service, a repository, or Spring's own request-handling
 * machinery) and turns it into the single {@link ErrorResponse} JSON shape
 * used across the whole API: {"message": ..., "status": ...}.
 * <p>
 * Without this class, an uncaught exception would either surface as
 * Spring Boot's default error page/JSON (a different, much more detailed
 * shape that leaks internal details like the exception's class name and
 * stack trace) or, for unexpected runtime exceptions, a raw 500 with a
 * generic body. Centralizing this here means individual controllers and
 * services never need to write their own try/catch blocks just to shape
 * an error response — they just throw, and this class is the only place
 * that maps "which exception" to "what the client sees."
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Every business-rule exception the service layer throws extends
     * {@link ApiException}, which already carries the correct HTTP status
     * for its case (404 for not-found, 409 for duplicates/conflicts, 401
     * for bad credentials, 403 for forbidden actions, etc.) — so this one
     * handler covers all of them without an instanceof chain.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getMessage(), ex.getStatus().value()));
    }

    /**
     * Thrown automatically by Spring when a {@code @Valid @RequestBody}
     * argument fails one of its bean-validation annotations (@NotBlank,
     * @Email, @DecimalMin, etc.), before the controller method body ever
     * runs. Spring's default body for this is a much more verbose,
     * differently-shaped JSON object listing every failed field — this
     * handler collapses it down to the same {message, status} shape as
     * every other error, joining all field errors into one readable
     * message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed.";
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * Last-resort safety net for anything not already handled above —
     * a bug, an unexpected repository/database exception, etc. Deliberately
     * generic on the message so internal details (class names, SQL,
     * stack traces) never reach the client; the real exception should
     * still be logged server-side for debugging (left as a TODO: wire in
     * a logger here once one is set up for the project).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        // TODO: log ex server-side (e.g. via SLF4J) once logging is set up.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
