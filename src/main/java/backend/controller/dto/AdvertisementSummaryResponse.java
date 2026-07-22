package backend.controller.dto;

import java.math.BigDecimal;

/**
 * Shortened advertisement representation used for the list endpoint
 * (GET /api/advertisements). Keeps list payloads small.
 */
public record AdvertisementSummaryResponse(
        Long id,
        String title,
        BigDecimal price,
        String cityName,
        String categoryName,
        String status,
        Double sellerRating
) {
}
