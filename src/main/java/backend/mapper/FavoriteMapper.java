package backend.mapper;

import backend.controller.dto.FavoriteResponse;
import backend.model.entity.Favorite;

/** Manual entity-to-DTO mapping for {@link Favorite}, same convention as {@link AdvertisementMapper}. */
public final class FavoriteMapper {

    private FavoriteMapper() {
    }

    public static FavoriteResponse toResponse(Favorite favorite) {
        return new FavoriteResponse(favorite.getId(), AdvertisementMapper.toSummary(favorite.getAdvertisement()));
    }
}
