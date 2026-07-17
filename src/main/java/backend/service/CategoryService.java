package backend.service;

import backend.exception.DuplicateResourceException;
import backend.exception.ResourceNotFoundException;
import backend.model.entity.Category;
import backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reference-data service for {@link Category}. No auth dependency for the
 * read methods — categories are freely browsable; {@link #create} is
 * intended to be admin-only, but (like the rest of the plan's services)
 * that restriction is enforced at the controller/security layer (Part 8 /
 * {@code SecurityConfig}'s {@code /api/admin/**} rule), not here.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** Only categories with no parent, i.e. the top level of the hierarchy. */
    @Transactional(readOnly = true)
    public List<Category> listTopLevel() {
        return categoryRepository.findByParentIsNull();
    }

    /**
     * The children of a given category. The parent must exist — a bad id
     * fails loudly with a 404 rather than silently returning an empty list,
     * which would be indistinguishable from "this category has no
     * subcategories".
     */
    @Transactional(readOnly = true)
    public List<Category> listChildren(Long parentId) {
        getById(parentId);
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found."));
    }

    /**
     * Creates a new category. {@code name} must be unique; if
     * {@code parentId} is given it must reference an existing category
     * (making the new one a subcategory of it).
     */
    @Transactional
    public Category create(String name, Long parentId) {
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists.");
        }

        Category category = new Category();
        category.setName(name);
        if (parentId != null) {
            category.setParent(getById(parentId));
        }

        categoryRepository.save(category);
        return category;
    }
}
