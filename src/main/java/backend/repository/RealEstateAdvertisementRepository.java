package backend.repository;

import backend.model.entity.RealEstateAdvertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for the real-estate-specific ad fields (JOINED inheritance
 * subtype of {@link backend.model.entity.Advertisement}).
 */
public interface RealEstateAdvertisementRepository extends JpaRepository<RealEstateAdvertisement, Long> {

    List<RealEstateAdvertisement> findByRoomsGreaterThanEqual(Integer minRooms);

    List<RealEstateAdvertisement> findByAreaSqmBetween(Double minAreaSqm, Double maxAreaSqm);
}
