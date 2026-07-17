package backend.controller;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.CreateAdvertisementRequest;
import backend.model.enums.AdvertisementStatus;
import backend.model.enums.Role;
import backend.security.AuthenticatedUser;
import backend.service.AdvertisementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for advertisements.
 *
 * IMPORTANT: This controller intentionally contains no business logic,
 * no repository access and no database queries. All of that lives in
 * {@link AdvertisementService}; this class only translates HTTP <->
 * service calls, resolving the caller's identity from the
 * {@link AuthenticatedUser} principal that {@code JwtAuthenticationFilter}
 * placed into the SecurityContext (see that class's Javadoc), and never
 * from client-supplied fields like an ownerId in the request body.
 */
@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    public AdvertisementController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    /**
     * Public listing of every ACTIVE advertisement. Unwraps the service's
     * {@code Page} into a flat list since the JavaFX client (see
     * {@code frontend.controller.AdvertisementListController}) parses
     * this response as a plain JSON array — once the client is updated to
     * send/understand pagination, this can start returning the Page
     * itself instead of {@code .getContent()}.
     */
    @GetMapping
    public List<AdvertisementSummaryResponse> getActiveAdvertisements(Pageable pageable) {
        return advertisementService.getActiveAdvertisements(pageable).getContent();
    }

    /**
     * The caller's own advertisements, in any status. Requires
     * authentication (see SecurityConfig: {@code GET /api/advertisements/my}
     * is carved out from the public read rule), so {@code user} is never
     * null here.
     */
    @GetMapping("/my")
    public Page<AdvertisementSummaryResponse> getMyAdvertisements(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) AdvertisementStatus status,
            Pageable pageable) {
        return advertisementService.getMyAdvertisements(user.userId(), status, pageable);
    }

    /**
     * Single advertisement's detail view. This route is public, so
     * {@code user} may be null (anonymous caller); the service itself
     * decides whether a non-ACTIVE ad is visible to this particular
     * caller (owner/admin only), returning the same 404 either way so
     * existence of a hidden ad is never leaked.
     */
    @GetMapping("/{id}")
    public AdvertisementDetailResponse getAdvertisementById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long currentUserId = (user != null) ? user.userId() : null;
        boolean isAdmin = (user != null) && Role.ADMIN.name().equals(user.role());
        return advertisementService.getById(id, currentUserId, isAdmin);
    }

    /**
     * Creates a new advertisement, owned by the authenticated caller.
     * Requires authentication (falls under SecurityConfig's default
     * "everything else needs a real user" rule), so {@code user} is never
     * null here.
     */
    @PostMapping
    public AdvertisementDetailResponse createAdvertisement(
            @Valid @RequestBody CreateAdvertisementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return advertisementService.create(request, user.userId());
    }

    /** Marks the caller's own ACTIVE advertisement as SOLD. */
    @PatchMapping("/{id}/sold")
    public AdvertisementDetailResponse markAsSold(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return advertisementService.markAsSold(id, user.userId());
    }

    /** Soft-deletes an advertisement. Owner or admin only. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdvertisement(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean isAdmin = Role.ADMIN.name().equals(user.role());
        advertisementService.deleteAdvertisement(id, user.userId(), isAdmin);
    }
}
