package backend.controller;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.RejectAdvertisementRequest;
import backend.controller.dto.UserResponse;
import backend.mapper.UserMapper;
import backend.model.enums.AccountStatus;
import backend.security.AuthenticatedUser;
import backend.service.AdvertisementService;
import backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for admin-only actions: advertisement moderation and
 * user moderation (plan §"Moderation" — grouped into one controller for
 * the same reason the plan groups them into one phase: both need
 * role-based ADMIN authorization). Every route here lives under
 * {@code /api/admin/**}, which {@code SecurityConfig} already restricts
 * to callers holding the {@code ADMIN} role — no per-method
 * {@code @PreAuthorize} needed here, unlike
 * {@code CategoryController}/{@code CityController}'s {@code create}
 * endpoints, which live outside {@code /api/admin/**} and so can't rely
 * on a URL-pattern rule.
 * <p>
 * Like every other controller, this one contains no business logic —
 * moderation rules (which status transitions are valid, admins not
 * blocking other admins, etc.) live in {@link AdvertisementService} and
 * {@link UserService}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdvertisementService advertisementService;
    private final UserService userService;

    public AdminController(AdvertisementService advertisementService, UserService userService) {
        this.advertisementService = advertisementService;
        this.userService = userService;
    }

    /** The moderation queue: every advertisement awaiting review. */
    @GetMapping("/advertisements/pending")
    public Page<AdvertisementSummaryResponse> listPendingReview(Pageable pageable) {
        return advertisementService.listPendingReview(pageable);
    }

    @PatchMapping("/advertisements/{id}/approve")
    public AdvertisementDetailResponse approve(@PathVariable Long id) {
        return advertisementService.approve(id);
    }

    @PatchMapping("/advertisements/{id}/reject")
    public AdvertisementDetailResponse reject(@PathVariable Long id,
                                               @Valid @RequestBody RejectAdvertisementRequest request) {
        return advertisementService.reject(id, request.reason());
    }

    /** Every registered user, optionally narrowed to one account status. */
    @GetMapping("/users")
    public List<UserResponse> listUsers(@RequestParam(required = false) AccountStatus status) {
        return userService.listUsers(status).stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    /**
     * Blocks a user's account. {@code admin} (the caller) is passed
     * through so the service can refuse an admin blocking themselves or
     * another admin.
     */
    @PatchMapping("/users/{id}/block")
    public UserResponse blockUser(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser admin) {
        return UserMapper.toResponse(userService.blockUser(id, admin.userId()));
    }

    @PatchMapping("/users/{id}/unblock")
    public UserResponse unblockUser(@PathVariable Long id) {
        return UserMapper.toResponse(userService.unblockUser(id));
    }
}
