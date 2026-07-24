package backend.repository;

import backend.model.entity.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Favorite}. A (user, advertisement) pair is unique
 * at the database level, so existence should be checked before saving a
 * new favorite to avoid a constraint-violation round trip.
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndAdvertisementId(Long userId, Long advertisementId);

    boolean existsByUserIdAndAdvertisementId(Long userId, Long advertisementId);

    /**
     * A user's favorites list needs to render a summary card for each
     * saved ad, so {@code advertisement} is fetched eagerly to avoid an
     * N+1 lazy load per row. Ordered newest-favorited-first.
     */
    @EntityGraph(attributePaths = "advertisement")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserIdAndAdvertisementId(Long userId, Long advertisementId);

    long countByAdvertisementId(Long advertisementId);
}