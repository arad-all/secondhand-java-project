package backend.service;

import backend.exception.DuplicateResourceException;
import backend.exception.ResourceNotFoundException;
import backend.model.entity.City;
import backend.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reference-data service for {@link City}. Like {@link CategoryService},
 * reads have no auth dependency; {@link #create} is intended to be
 * admin-only, enforced at the controller/security layer rather than here.
 */
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<City> listAll() {
        return cityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public City getById(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City with id " + id + " not found."));
    }

    /** Creates a new city. {@code name} must be unique. */
    @Transactional
    public City create(String name, String province) {
        if (cityRepository.existsByName(name)) {
            throw new DuplicateResourceException("City '" + name + "' already exists.");
        }

        City city = new City();
        city.setName(name);
        city.setProvince(province);

        cityRepository.save(city);
        return city;
    }
}
