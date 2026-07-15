package backend.mapper;

import backend.controller.dto.CityResponse;
import backend.model.entity.City;

public final class CityMapper {

    private CityMapper() {
    }

    public static CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getProvince());
    }
}
