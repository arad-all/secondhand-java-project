package backend.controller.dto;

/**
 * Response body for POST /api/auth/login.
 * Once a real AuthService exists, "token" should hold a signed JWT.
 */
public record LoginResponse(String token, Long userId, String username, String role) {
}
