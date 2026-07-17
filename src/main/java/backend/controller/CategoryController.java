package backend.controller;

import backend.controller.dto.CategoryResponse;
import backend.controller.dto.CreateCategoryRequest;
import backend.mapper.CategoryMapper;
import backend.model.entity.Category;
import backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the category reference data. Reads are public (see
 * SecurityConfig's permitAll rule for GET /api/categories/**); creating a
 * category is admin-only. That path isn't under /api/admin/**, so it's
 * enforced with @PreAuthorize here instead of a URL-pattern rule, exactly
 * as SecurityConfig's Javadoc describes.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Top-level categories only (no parent). Use /{id}/children for subcategories. */
    @GetMapping
    public List<CategoryResponse> listTopLevel() {
        return categoryService.listTopLevel().stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return CategoryMapper.toResponse(categoryService.getById(id));
    }

    /** The children of the given category. 404s if the parent id itself doesn't exist. */
    @GetMapping("/{id}/children")
    public List<CategoryResponse> listChildren(@PathVariable Long id) {
        return categoryService.listChildren(id).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.create(request.name(), request.parentId());
        return CategoryMapper.toResponse(category);
    }
}
