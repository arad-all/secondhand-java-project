package backend.repository;

import backend.model.entity.ElectronicsAdvertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for the electronics-specific ad fields (JOINED inheritance
 * subtype of {@link backend.model.entity.Advertisement}).
 */
public interface ElectronicsAdvertisementRepository extends JpaRepository<ElectronicsAdvertisement, Long> {

    List<ElectronicsAdvertisement> findByBrandIgnoreCase(String brand);

    List<ElectronicsAdvertisement> findByWarrantyMonthsGreaterThanEqual(Integer minWarrantyMonths);
}
