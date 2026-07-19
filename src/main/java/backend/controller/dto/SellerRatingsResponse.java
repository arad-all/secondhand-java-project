package backend.controller.dto;

import java.util.List;

/**
 * A seller's reputation: how many ratings they've received, their average
 * score, and the ratings themselves. {@code averageScore} is {@code 0.0}
 * when {@code totalRatings} is {@code 0} (no ratings yet), never
 * {@code null} — simpler for clients than having to null-check it.
 */
public record SellerRatingsResponse(
        Long sellerId,
        double averageScore,
        long totalRatings,
        List<RatingResponse> ratings
) {
}
