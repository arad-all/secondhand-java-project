package backend.controller.dto;

/** Response shape for a city. */
public record CityResponse(Long id, String name, String province) {
}
