package backend.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the admin "reject advertisement" action
 * (PATCH /api/admin/advertisements/{id}/reject). The reason is stored on
 * the advertisement's {@code adminNote} field so the owner can see why
 * their ad was rejected.
 */
public record RejectAdvertisementRequest(
        @NotBlank String reason
) {
}
