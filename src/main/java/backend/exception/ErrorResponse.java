package backend.exception;

/**
 * The single JSON shape every error response is returned in, regardless
 * of which exception caused it. Kept as a plain record (same style as the
 * DTOs in {@code backend.controller.dto}) since it's just a data carrier.
 */
public record ErrorResponse(String message, int status) {
}
