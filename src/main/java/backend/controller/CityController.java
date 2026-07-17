package backend.controller;

import backend.controller.dto.CityResponse;
import backend.controller.dto.CreateCityRequest;
import backend.mapper.CityMapper;
import backend.model.entity.City;
import backend.service.CityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the city reference data. Reads are public (see
 * SecurityConfig's permitAll rule for GET /api/cities/**); creating a
 * city is admin-only. That path isn't under /api/admin/**, so it's
 * enforced with @PreAuthorize here instead of a URL-pattern rule, exactly
 * as SecurityConfig's Javadoc describes.
 */
@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public List<CityResponse> listAll() {
        return cityService.listAll().stream()
                .map(CityMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CityResponse getById(@PathVariable Long id) {
        return CityMapper.toResponse(cityService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CityResponse create(@Valid @RequestBody CreateCityRequest request) {
        City city = cityService.create(request.name(), request.province());
        return CityMapper.toResponse(city);
    }
}
