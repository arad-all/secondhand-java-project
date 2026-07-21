package backend.controller;

import backend.controller.dto.CreateRatingRequest;
import backend.controller.dto.RatingResponse;
import backend.controller.dto.SellerRatingsResponse;
import backend.security.AuthenticatedUser;
import backend.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for ratings. Deliberately its own controller rather than
 * folded into {@code AdvertisementController} — a rating is about the
 * seller's reputation, not the advertisement's own lifecycle, and it
 * touches two different URL prefixes ({@code /api/advertisements/**} to
 * submit, {@code /api/users/**} to view), same grouping reasoning
 * {@code AdminController} already uses for two different resource types.
 * <p>
 * {@code GET /api/users/{id}/ratings} is carved out as public in
 * {@code SecurityConfig} — seeing a seller's reputation shouldn't require
 * logging in, same as browsing ads does not. Submitting a rating still
 * requires authentication under the default rule, since {@code buyerId}
 * has to come from a real caller.
 */
@RestController
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /** A seller's reputation: average score, total count, and every rating received. Public. */
    @GetMapping("/api/users/{sellerId}/ratings")
    public SellerRatingsResponse getSellerRatings(@PathVariable Long sellerId) {
        return ratingService.getSellerRatings(sellerId);
    }

    /**
     * Rates the seller of a SOLD advertisement. The buyer is always the
     * authenticated caller — never taken from the request body.
     */
    @PostMapping("/api/advertisements/{advertisementId}/ratings")
    public RatingResponse rate(
            @PathVariable Long advertisementId,
            @Valid @RequestBody CreateRatingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ratingService.rate(advertisementId, user.userId(), request);
    }
}
