package backend.repository;

import backend.model.entity.AdvertisementImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for {@link AdvertisementImage}. Images are ordered for
 * display via {@code displayOrder}.
 */
public interface AdvertisementImageRepository extends JpaRepository<AdvertisementImage, Long> {

    List<AdvertisementImage> findByAdvertisementIdOrderByDisplayOrderAsc(Long advertisementId);

    long countByAdvertisementId(Long advertisementId);

    void deleteByAdvertisementId(Long advertisementId);
}
