package backend.mapper;

import backend.controller.dto.SellerProfileResponse;
import backend.controller.dto.UserResponse;
import backend.model.entity.User;

/**
 * Manual entity-to-DTO mapping for {@link User}, mirroring the rest of
 * this package's mappers (e.g. {@link CategoryMapper}): mapping lives in
 * a small dedicated class rather than scattered inline in a service or
 * controller.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name());
    }

    /** Public-safe subset of {@link #toResponse}, for the seller-profile endpoint. */
    public static SellerProfileResponse toSellerProfile(User user) {
        return new SellerProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber());
    }
}
