package backend.controller.dto;

import java.math.BigDecimal;

/**
 * Shortened advertisement representation used for the list endpoint
 * (GET /api/advertisements). Keeps list payloads small.
 * {@code firstImageUrl} is {@code null} when the advertisement has no images.
 */
public record AdvertisementSummaryResponse(
        Long id,
        String title,
        BigDecimal price,
        String cityName,
        String categoryName,
        String status,
        Double sellerRating,
        String firstImageUrl
) {
}
