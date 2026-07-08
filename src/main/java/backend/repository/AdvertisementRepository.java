package backend.repository;

import backend.model.entity.Advertisement;
import backend.model.enums.AdvertisementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Advertisement}. Advertisement is the entity most
 * pages will list/filter/browse, so this repository provides:
 *   - simple derived-query lookups for the common single-field filters,
 *   - a fetch-joined lookup for a detail page (avoids N+1 on images),
 *   - a combined, multi-filter search for the browse/listing page, where
 *     any optional filter can be left unset (null) to mean "don't filter
 *     on this" — status is the one exception and is always required, so
 *     pending/rejected/sold ads can never leak into a public listing just
 *     because a caller forgot to pass ACTIVE.
 *
 * Every list-returning finder below is annotated with @EntityGraph to
 * eagerly fetch owner/category/city in the same query. Card/list views
 * always need to show the owner's name, category, and city alongside each
 * ad — without this, each row in a list of N ads would trigger up to 3
 * extra lazy-loading queries (classic N+1), which neither the ID-based nor
 * the entity-based version of this repository addressed for anything
 * other than the single-item detail lookup.
 */
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @EntityGraph(attributePaths = {"owner", "category", "city"})
    Page<Advertisement> findByStatus(AdvertisementStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "category", "city"})
    List<Advertisement> findByOwnerId(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "category", "city"})
    Page<Advertisement> findByOwnerIdAndStatus(Long ownerId, AdvertisementStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "category", "city"})
    List<Advertisement> findByCategoryIdAndStatus(Long categoryId, AdvertisementStatus status);

    @EntityGraph(attributePaths = {"owner", "category", "city"})
    List<Advertisement> findByCityIdAndStatus(Long cityId, AdvertisementStatus status);

    /** Loads one advertisement together with its images in a single query. */
    @Query("SELECT DISTINCT a FROM Advertisement a LEFT JOIN FETCH a.images WHERE a.id = :id")
    Optional<Advertisement> findByIdWithImages(@Param("id") Long id);

    /**
     * Backing query for {@link #search}. Not intended to be called
     * directly: {@code pattern} must already be wrapped with '%' wildcards
     * (or be null), which the default method below takes care of so
     * callers only ever deal with a plain keyword. {@code status} is
     * required (not nullable) so this can never be used to accidentally
     * search across every status, including ones that shouldn't be
     * publicly visible.
     */
    @EntityGraph(attributePaths = {"owner", "category", "city"})
    @Query("""
            SELECT a FROM Advertisement a
            WHERE a.status = :status
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
              AND (:cityId IS NULL OR a.city.id = :cityId)
              AND (:minPrice IS NULL OR a.price >= :minPrice)
              AND (:maxPrice IS NULL OR a.price <= :maxPrice)
              AND (:pattern IS NULL
                   OR LOWER(a.title) LIKE LOWER(:pattern)
                   OR LOWER(a.description) LIKE LOWER(:pattern))
            """)
    Page<Advertisement> searchInternal(@Param("status") AdvertisementStatus status,
                                        @Param("categoryId") Long categoryId,
                                        @Param("cityId") Long cityId,
                                        @Param("minPrice") BigDecimal minPrice,
                                        @Param("maxPrice") BigDecimal maxPrice,
                                        @Param("pattern") String pattern,
                                        Pageable pageable);

    /**
     * Multi-filter search for the browse page. {@code status} must be
     * supplied by the caller (the service layer should default this to
     * {@code ACTIVE} for any public-facing search) and is always applied,
     * so pending/rejected/sold ads can't leak into results. Every other
     * parameter can be {@code null} to skip that filter; {@code keyword}
     * is matched case-insensitively against both the title and the
     * description.
     */
    default Page<Advertisement> search(AdvertisementStatus status,
                                        Long categoryId,
                                        Long cityId,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        String keyword,
                                        Pageable pageable) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null - pass the status to scope search to " +
                    "(e.g. ACTIVE for public search) so non-public ads can't leak into results");
        }
        String pattern = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";
        return searchInternal(status, categoryId, cityId, minPrice, maxPrice, pattern, pageable);
    }
}
