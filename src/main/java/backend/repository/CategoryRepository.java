package backend.repository;

import backend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Category}. Categories can optionally form a
 * parent/child hierarchy, so lookups for top-level categories and for
 * the children of a given parent are provided alongside the basic
 * name lookup (name is unique).
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    /** Top-level categories, i.e. those with no parent. */
    List<Category> findByParentIsNull();

    List<Category> findByParentId(Long parentId);

    /**
     * {@code categoryId} plus every descendant's id, recursively — used
     * by {@code AdvertisementService#search} so filtering by a parent
     * category (e.g. "Electronics") also matches ads filed directly
     * under one of its subcategories (e.g. "Cell Phones"), not just ads
     * filed under "Electronics" itself.
     * <p>
     * Deliberately doesn't check {@code categoryId} exists: a nonexistent
     * id simply has no children via {@link #findByParentId}, so it
     * resolves to a single-element list that matches nothing — the same
     * "no error, just no results" behavior a plain equality filter on a
     * bad id already has today.
     * <p>
     * Safe to recurse without cycle detection — a category's parent must
     * reference an already-existing row at creation time
     * ({@code CategoryService#create}), and there's no "reparent a
     * category" endpoint, so a cycle can never be constructed.
     */
    default List<Long> findIdsIncludingDescendants(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        collectDescendantIds(categoryId, ids);
        return ids;
    }

    private void collectDescendantIds(Long categoryId, List<Long> accumulator) {
        accumulator.add(categoryId);
        for (Category child : findByParentId(categoryId)) {
            collectDescendantIds(child.getId(), accumulator);
        }
    }
}