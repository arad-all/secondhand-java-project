package backend.controller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full advertisement representation used for the details endpoint
 * (GET /api/advertisements/{id}) and returned after creating an ad.
 */
public record AdvertisementDetailResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        String cityName,
        String categoryName,
        String status,
        String ownerUsername,
        List<String> imageUrls
) {
}
