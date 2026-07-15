package backend.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/categories (admin-only, enforced via
 * {@code @PreAuthorize} on the controller method). {@code parentId} is
 * optional — omit it to create a top-level category.
 */
public record CreateCategoryRequest(
        @NotBlank String name,
        Long parentId
) {
}
