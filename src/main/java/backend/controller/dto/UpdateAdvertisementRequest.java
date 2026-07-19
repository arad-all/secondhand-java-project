package backend.controller.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Request body for {@code PATCH /api/advertisements/{id}}. Partial update:
 * every field is optional, and only the ones supplied (non-null) overwrite
 * the existing advertisement. No {@code ownerId} or {@code status} field —
 * ownership is resolved server-side, and status changes go through their
 * own dedicated endpoints.
 */
public record UpdateAdvertisementRequest(
        String title,
        String description,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        Long categoryId,
        Long cityId
) {
}