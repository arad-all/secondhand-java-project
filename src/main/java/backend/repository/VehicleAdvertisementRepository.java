package backend.repository;

import backend.model.entity.VehicleAdvertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for the vehicle-specific ad fields (JOINED inheritance
 * subtype of {@link backend.model.entity.Advertisement}).
 */
public interface VehicleAdvertisementRepository extends JpaRepository<VehicleAdvertisement, Long> {

    List<VehicleAdvertisement> findByBrandIgnoreCase(String brand);

    List<VehicleAdvertisement> findByManufactureYearBetween(Integer fromYear, Integer toYear);

    List<VehicleAdvertisement> findByMileageKmLessThanEqual(Integer maxMileageKm);
}
