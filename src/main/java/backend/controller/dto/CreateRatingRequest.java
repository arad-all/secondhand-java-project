package backend.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request body for {@code POST /api/advertisements/{id}/ratings} — a
 * buyer rating the seller of a SOLD advertisement. No {@code buyerId} or
 * {@code sellerId} field: the buyer is resolved from the authenticated
 * caller and the seller from the advertisement's own owner, never from
 * client input.
 */
public record CreateRatingRequest(
        @Min(1) @Max(5) int score,
        String comment
) {
}
