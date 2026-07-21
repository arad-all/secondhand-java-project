package backend.controller;

import backend.controller.dto.*;
import backend.model.enums.AdvertisementStatus;
import backend.model.enums.Role;
import backend.security.AuthenticatedUser;
import backend.service.AdvertisementService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
    public List<AdvertisementSummaryResponse> getActiveAdvertisements(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return advertisementService.getActiveAdvertisements(pageable).getContent();
    }

    /**
     * Public keyword/filter search — category, city, price range, and a
     * free-text keyword against title/description — on top of the plain
     * listing {@link #getActiveAdvertisements} provides.
     * <p>
     * {@code status} defaults to {@code ACTIVE}; whether a non-ACTIVE value
     * is actually honored is decided by {@link AdvertisementService#search}
     * based on {@code isAdmin}, resolved here from the JWT the same way
     * {@link #getAdvertisementById} already does.
     */
    @GetMapping("/search")
    public Page<AdvertisementSummaryResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "ACTIVE") AdvertisementStatus status,
            @AuthenticationPrincipal AuthenticatedUser user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        boolean isAdmin = (user != null) && Role.ADMIN.name().equals(user.role());
        return advertisementService.search(keyword, categoryId, cityId, minPrice, maxPrice, status, isAdmin, pageable);
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
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
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
     * Edits an advertisement's own fields. Partial update — only fields
     * present in the body are changed. Owner-or-admin check happens in
     * the service; this just resolves {@code isAdmin} from the JWT.
     */
    @PatchMapping("/{id}")
    public AdvertisementDetailResponse editAdvertisement(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdvertisementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean isAdmin = Role.ADMIN.name().equals(user.role());
        return advertisementService.editAdvertisement(id, request, user.userId(), isAdmin);
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
            @Valid @RequestBody MarkAsSoldRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return advertisementService.markAsSold(id, user.userId(), request.buyerId());
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

    /**
     * Adds one or more images to the caller's own advertisement. Requires
     * authentication (falls under SecurityConfig's default "everything
     * else needs a real user" rule, same as {@link #createAdvertisement}),
     * so {@code user} is never null here; owner-only enforcement happens
     * in the service. {@code files} is deliberately optional at the HTTP
     * layer ({@code required = false}) so a request with no {@code files}
     * part gets the service's own {@code InvalidFileException} (a clean
     * 400) instead of Spring's generic missing-parameter exception, which
     * {@code GlobalExceptionHandler} doesn't special-case.
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdvertisementDetailResponse addImages(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return advertisementService.addImages(id, files, user.userId());
    }

    /**
     * Serves a previously uploaded image's raw bytes. Public, like the
     * other {@code GET /api/advertisements/**} routes (see SecurityConfig)
     * — anyone who has an image's URL (e.g. from an
     * {@link AdvertisementDetailResponse}) can load it directly, the same
     * way a plain image-hosting URL would work, rather than requiring a
     * second authenticated round trip just to view a picture.
     */
    @GetMapping("/{id}/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id, @PathVariable String filename) {
        Resource resource = advertisementService.loadImage(id, filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
