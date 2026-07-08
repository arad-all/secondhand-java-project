package backend.repository;

import backend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
