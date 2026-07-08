package backend.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/register.
 * Plain record used only to shape the JSON request; no business logic here.
 */
public record RegisterRequest(
        @NotBlank String fullName,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String phoneNumber,
        @Email String email
) {
}
