package backend.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for POST /api/advertisements.
 * Note: there is deliberately no "ownerId" field here — once a real
 * AuthService/JWT filter exists, the owner must be resolved from the
 * authenticated user's token, never trusted from client input.
 */
public record CreateAdvertisementRequest(
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull Long categoryId,
        @NotNull Long cityId
) {
}
