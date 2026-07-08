package backend.repository;

import backend.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link City}.
 */
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByName(String name);

    boolean existsByName(String name);

    List<City> findByProvince(String province);
}
