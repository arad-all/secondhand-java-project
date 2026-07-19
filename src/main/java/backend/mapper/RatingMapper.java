package backend.mapper;

import backend.controller.dto.RatingResponse;
import backend.model.entity.Rating;

/** Manual entity-to-DTO mapping for {@link Rating}, same convention as {@link AdvertisementMapper}. */
public final class RatingMapper {

    private RatingMapper() {
    }

    public static RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getAdvertisement().getId(),
                rating.getAdvertisement().getTitle(),
                rating.getBuyer().getUsername(),
                rating.getSeller().getUsername(),
                rating.getScore(),
                rating.getComment());
    }
}
