package backend.controller.dto;

/**
 * Public-facing profile of a seller, shown on the JavaFX client's
 * "seller profile" page (opened from an advertisement's detail view).
 * Deliberately narrower than {@link UserResponse} (which is
 * admin-only and also carries email/role/account status): a seller's
 * profile only ever needs to reveal what a prospective buyer would
 * reasonably see — username, display name and phone number — not
 * moderation-sensitive fields like account status or role.
 */
public record SellerProfileResponse(
        Long id,
        String username,
        String fullName,
        String phoneNumber
) {
}
