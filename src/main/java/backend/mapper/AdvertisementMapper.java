package backend.mapper;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.model.entity.Advertisement;
import backend.model.entity.AdvertisementImage;

import java.util.Comparator;
import java.util.List;

/**
 * Manual entity-to-DTO mapping for {@link Advertisement}, kept out of
 * {@code AdvertisementService} per the plan's guidance: mapping lives in a
 * small dedicated class, not scattered inline across service methods.
 * <p>
 * Phase 1 scope (plan §0.2): only the base {@link Advertisement} entity's
 * fields are mapped here. Once the subtype phase lands, this is where an
 * {@code instanceof VehicleAdvertisement} / etc. check would be added to
 * fill in an extended detail DTO.
 */
public final class AdvertisementMapper {

    private AdvertisementMapper() {
    }

    /** Card/list view — excludes description and full image list to keep list payloads small,
     *  but includes the first image URL (if any) so the UI can show a thumbnail. */
    public static AdvertisementSummaryResponse toSummary(Advertisement ad) {
        String firstImageUrl = ad.getImages().stream()
                .sorted(Comparator.comparing(
                        AdvertisementImage::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .map(image -> "/api/advertisements/" + ad.getId() + "/images/" + image.getImagePath())
                .orElse(null);

        return new AdvertisementSummaryResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getPrice(),
                ad.getCity().getName(),
                ad.getCategory().getName(),
                ad.getStatus().name(),
                ad.getSellerRating(),
                firstImageUrl);
    }

    /**
     * Full detail view, including image URLs in display order. Each URL
     * is the path to {@code AdvertisementController#getImage}, built from
     * the advertisement's id and the image's stored filename — callers
     * never see the raw on-disk filename ({@link AdvertisementImage#getImagePath()})
     * by itself, only this resolvable path.
     */
    public static AdvertisementDetailResponse toDetail(Advertisement ad) {
        List<String> imageUrls = ad.getImages().stream()
                .sorted(Comparator.comparing(
                        AdvertisementImage::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(image -> "/api/advertisements/" + ad.getId() + "/images/" + image.getImagePath())
                .toList();

        return new AdvertisementDetailResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getDescription(),
                ad.getPrice(),
                ad.getCity().getName(),
                ad.getCategory().getName(),
                ad.getStatus().name(),
                ad.getOwner().getUsername(),
                ad.getBuyer() != null ? ad.getBuyer().getUsername() : null,
                ad.getAdminNote(),
                imageUrls);
    }
}
