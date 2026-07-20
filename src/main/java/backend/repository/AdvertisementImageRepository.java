package backend.repository;

import backend.model.entity.AdvertisementImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link AdvertisementImage}. Images are ordered for
 * display via {@code displayOrder}.
 */
public interface AdvertisementImageRepository extends JpaRepository<AdvertisementImage, Long> {

    List<AdvertisementImage> findByAdvertisementIdOrderByDisplayOrderAsc(Long advertisementId);

    long countByAdvertisementId(Long advertisementId);

    void deleteByAdvertisementId(Long advertisementId);

    /**
     * Confirms a given stored filename actually belongs to this
     * advertisement before it's served back. Used by the image-download
     * endpoint so a filename can't be used to read a file associated with
     * a different advertisement, and so a row deleted from the database
     * (e.g. by a future "remove image" feature) stops being servable even
     * if the file briefly still exists on disk.
     */
    Optional<AdvertisementImage> findByAdvertisementIdAndImagePath(Long advertisementId, String imagePath);
}
