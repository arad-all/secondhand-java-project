package backend.controller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full advertisement representation used for the details endpoint
 * (GET /api/advertisements/{id}) and returned after creating an ad.
 * {@code adminNote} is non-null only after an admin rejects the ad (see
 * {@code AdvertisementService#reject}) — it's cleared again on approval —
 * so the owner can see why their advertisement was turned down.
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
        String buyerUsername,
        String adminNote,
        List<String> imageUrls
) {
}
