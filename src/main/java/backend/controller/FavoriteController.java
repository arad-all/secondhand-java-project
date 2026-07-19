package backend.controller;

import backend.controller.dto.FavoriteResponse;
import backend.security.AuthenticatedUser;
import backend.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the caller's own favorites (saved ads). Every route
 * here falls under {@code SecurityConfig}'s default "everything else
 * needs a real user" rule — there's no public route under
 * {@code /api/favorites/**} — so {@code user} is never null below, and
 * there's no separate admin/cross-user variant since a favorites list
 * only ever makes sense scoped to its owner.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** The caller's saved ads. */
    @GetMapping
    public List<FavoriteResponse> getMyFavorites(@AuthenticationPrincipal AuthenticatedUser user) {
        return favoriteService.getMyFavorites(user.userId());
    }

    /** Saves an advertisement to the caller's favorites. */
    @PostMapping("/{advertisementId}")
    public FavoriteResponse addFavorite(
            @PathVariable Long advertisementId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return favoriteService.addFavorite(user.userId(), advertisementId);
    }

    /** Removes an advertisement from the caller's favorites. */
    @DeleteMapping("/{advertisementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable Long advertisementId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        favoriteService.removeFavorite(user.userId(), advertisementId);
    }
}
