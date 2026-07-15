package backend.controller.dto;

/** Response shape for a category. {@code parentId} is null for a top-level category. */
public record CategoryResponse(Long id, String name, Long parentId) {
}
