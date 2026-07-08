package backend.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/login.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
