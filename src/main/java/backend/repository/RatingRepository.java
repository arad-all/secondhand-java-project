package backend.repository;

import backend.model.entity.Rating;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Rating}. A (advertisement, buyer) pair is unique
 * at the database level: one rating per buyer per ad.
 */
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByAdvertisementIdAndBuyerId(Long advertisementId, Long buyerId);

    boolean existsByAdvertisementIdAndBuyerId(Long advertisementId, Long buyerId);

    /**
     * A seller's ratings/reviews list needs to show who left each rating
     * and which ad it was for, so {@code buyer}, {@code seller}, and
     * {@code advertisement} — everything {@code RatingMapper#toResponse}
     * needs — are fetched eagerly to avoid an N+1 lazy load per row.
     */
    @EntityGraph(attributePaths = {"buyer", "seller", "advertisement"})
    List<Rating> findBySellerId(Long sellerId);

    long countBySellerId(Long sellerId);

    /** Average rating (1-5) a seller has received; {@code null} if none. */
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.seller.id = :sellerId")
    Double findAverageScoreBySellerId(@Param("sellerId") Long sellerId);
}
