package backend.mapper;

import backend.controller.dto.CityResponse;
import backend.model.entity.City;

/**
 * Manual entity-to-DTO mapping for {@link City}, mirroring
 * {@link CategoryMapper}'s pattern: mapping lives in a small dedicated
 * class rather than scattered inline in the service or controller.
 */
public final class CityMapper {

    private CityMapper() {
    }

    public static CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getProvince());
    }
}
