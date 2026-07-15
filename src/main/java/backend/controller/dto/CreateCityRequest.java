package backend.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/cities (admin-only, enforced via
 * {@code @PreAuthorize} on the controller method). {@code province} is
 * optional.
 */
public record CreateCityRequest(
        @NotBlank String name,
        String province
) {
}
