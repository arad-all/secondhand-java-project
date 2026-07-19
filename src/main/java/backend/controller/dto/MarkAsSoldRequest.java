package backend.controller.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/advertisements/{id}/sold}. Records
 * who bought the ad — this is what later lets
 * {@code RatingService#rate} confirm a rating actually comes from the
 * buyer, not just anyone.
 */
public record MarkAsSoldRequest(
        @NotNull Long buyerId
) {
}
