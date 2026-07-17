package backend.controller.dto;

/**
 * Response shape for a user, as seen by an admin. Never includes the
 * password hash — nothing in this record could leak it even by accident.
 */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String phoneNumber,
        String email,
        String role,
        String status
) {
}
