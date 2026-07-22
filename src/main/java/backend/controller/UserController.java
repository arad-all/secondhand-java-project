package backend.controller;

import backend.controller.dto.SellerProfileResponse;
import backend.mapper.UserMapper;
import backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only user-profile lookups. Currently just the one route
 * the JavaFX client's "seller profile" page needs: given the
 * {@code ownerUsername}/{@code buyerUsername} a client already has from
 * an {@code AdvertisementDetailResponse} or {@code RatingResponse}, resolve
 * it to that user's public-safe profile (and, since the response includes
 * {@code id}, to the numeric id {@code GET /api/users/{id}/ratings}
 * needs).
 * <p>
 * Deliberately its own controller rather than folded into
 * {@code RatingController} — that one is scoped to ratings specifically,
 * even though its Javadoc already touches on {@code /api/users/**}. This
 * route is public in {@code SecurityConfig}, same reasoning as ad
 * browsing and seller ratings: viewing a public profile shouldn't
 * require logging in.
 * <p>
 * Contains no business logic of its own — {@link UserService#getByUsername}
 * (mirroring the already-existing {@code getById}) and
 * {@link UserMapper#toSellerProfile} do all the work; this class only
 * translates HTTP &lt;-&gt; service call, exactly like every other
 * controller in this codebase.
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/by-username/{username}")
    public SellerProfileResponse getByUsername(@PathVariable String username) {
        return UserMapper.toSellerProfile(userService.getByUsername(username));
    }
}
