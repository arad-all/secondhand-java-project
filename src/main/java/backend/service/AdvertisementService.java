package backend.service;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.CreateAdvertisementRequest;
import backend.exception.ForbiddenActionException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.AdvertisementMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.Category;
import backend.model.entity.City;
import backend.model.entity.User;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementRepository;
import backend.repository.CategoryRepository;
import backend.repository.CityRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Core advertisement business logic (plan §1, "AdvertisementService — the
 * core of the app"). This slice covers public browsing (search, getById)
 * and owner actions (create, getMyAdvertisements, markAsSold,
 * deleteAdvertisement); moderation (approve/reject/listPendingReview) is a
 * later phase.
 * <p>
 * Status lifecycle enforced by the methods below:
 * <pre>
 * PENDING_REVIEW --(admin approve)--&gt; ACTIVE            [later phase]
 * PENDING_REVIEW --(admin reject)---&gt; REJECTED           [later phase]
 * ACTIVE --(owner marks sold)-------&gt; SOLD
 * ACTIVE/PENDING_REVIEW/REJECTED --(owner or admin deletes)--&gt; DELETED
 * </pre>
 * Any transition not on this list (e.g. SOLD -&gt; ACTIVE, deleting an
 * already-DELETED or SOLD ad) is rejected with
 * {@link InvalidStateTransitionException}.
 * <p>
 * Phase 1 scope (plan §0.2): {@link #create} only ever instantiates the
 * base {@link Advertisement} entity — VehicleAdvertisement /
 * ElectronicsAdvertisement / RealEstateAdvertisement are intentionally
 * left unused here and picked up in a later phase.
 */
@Service
@RequiredArgsConstructor
public class AdvertisementService {

    /** Statuses a delete is allowed to transition *from* — see the lifecycle comment above. */
    private static final Set<AdvertisementStatus> DELETABLE_STATUSES =
            Set.of(AdvertisementStatus.ACTIVE, AdvertisementStatus.PENDING_REVIEW, AdvertisementStatus.REJECTED);

    private final AdvertisementRepository advertisementRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;

    /**
     * Public browse/search. Always forces {@code status = ACTIVE},
     * regardless of any caller input — the repository's {@code search()}
     * deliberately throws if status is null for exactly this reason, so
     * this is the one and only place that decides the value. Never let a
     * future refactor take status from the caller here.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> search(String keyword,
                                                       Long categoryId,
                                                       Long cityId,
                                                       BigDecimal minPrice,
                                                       BigDecimal maxPrice,
                                                       Pageable pageable) {
        Page<Advertisement> results = advertisementRepository.search(
                AdvertisementStatus.ACTIVE, categoryId, cityId, minPrice, maxPrice, keyword, pageable);
        return results.map(AdvertisementMapper::toSummary);
    }

    /**
     * Fetches one advertisement's detail view. An {@code ACTIVE} ad is
     * visible to anyone; anything else (pending review, rejected, sold,
     * deleted) is visible only to its owner or an admin. Any other caller
     * gets the exact same {@link ResourceNotFoundException} as a truly
     * missing id — never a 403 — so this never leaks whether a
     * non-visible ad exists.
     *
     * @param currentUserId the caller's id, or {@code null} if anonymous
     * @param isAdmin       whether the caller holds the ADMIN role
     */
    @Transactional(readOnly = true)
    public AdvertisementDetailResponse getById(Long id, Long currentUserId, boolean isAdmin) {
        Advertisement ad = advertisementRepository.findByIdWithImages(id)
                .orElseThrow(() -> notFound(id));

        boolean isOwner = currentUserId != null && ad.getOwner().getId().equals(currentUserId);
        if (ad.getStatus() != AdvertisementStatus.ACTIVE && !isOwner && !isAdmin) {
            throw notFound(id);
        }

        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * The caller's own advertisements, in whatever status they're in —
     * an owner needs to see PENDING_REVIEW/REJECTED ads too (e.g. to find
     * out why one was rejected), so this deliberately does not filter down
     * to ACTIVE like public search does. If {@code statusFilter} is given,
     * only ads in that status are returned.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> getMyAdvertisements(Long ownerId,
                                                                  AdvertisementStatus statusFilter,
                                                                  Pageable pageable) {
        Page<Advertisement> results = (statusFilter != null)
                ? advertisementRepository.findByOwnerIdAndStatus(ownerId, statusFilter, pageable)
                : advertisementRepository.findByOwnerId(ownerId, pageable);
        return results.map(AdvertisementMapper::toSummary);
    }

    /**
     * Creates a new advertisement. The owner is always resolved from
     * {@code ownerId} — the caller's authenticated id, resolved by the
     * controller from the JWT — never from the request body, which has no
     * such field by design. {@code categoryId}/{@code cityId} must
     * reference existing rows. The ad always starts at
     * {@code PENDING_REVIEW}; the entity's own default already is that,
     * but it's set explicitly here so the rule stays visible even if the
     * entity's default ever changes.
     * <p>
     * Phase 1 scope: only ever instantiates the base {@code Advertisement}
     * entity (see the class-level Javadoc).
     */
    @Transactional
    public AdvertisementDetailResponse create(CreateAdvertisementRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + ownerId + " not found."));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with id " + request.categoryId() + " not found."));
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "City with id " + request.cityId() + " not found."));

        Advertisement ad = new Advertisement();
        ad.setTitle(request.title());
        ad.setDescription(request.description());
        ad.setPrice(request.price());
        ad.setCategory(category);
        ad.setCity(city);
        ad.setOwner(owner);
        ad.setStatus(AdvertisementStatus.PENDING_REVIEW);

        advertisementRepository.save(ad);
        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * Marks the ad sold. Owner-only, and only a valid transition from
     * {@code ACTIVE} — trying to sell a PENDING_REVIEW/REJECTED/DELETED/
     * already-SOLD ad is rejected, not silently accepted.
     */
    @Transactional
    public AdvertisementDetailResponse markAsSold(Long adId, Long ownerId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        if (!ad.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenActionException("Only the ad's owner can mark it as sold.");
        }
        if (ad.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Cannot mark advertisement as SOLD from status " + ad.getStatus() + ".");
        }

        ad.setStatus(AdvertisementStatus.SOLD);
        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * Soft-deletes the ad — sets {@code status = DELETED} rather than a
     * hard row delete, so conversations/ratings/history tied to it
     * survive. Owner or admin only, and only a valid transition from
     * {@code ACTIVE}/{@code PENDING_REVIEW}/{@code REJECTED}; deleting an
     * already-{@code DELETED} or {@code SOLD} ad is rejected as an invalid
     * transition.
     */
    @Transactional
    public void deleteAdvertisement(Long adId, Long currentUserId, boolean isAdmin) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        boolean isOwner = ad.getOwner().getId().equals(currentUserId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenActionException("Only the ad's owner or an admin can delete this advertisement.");
        }
        if (!DELETABLE_STATUSES.contains(ad.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Cannot delete advertisement from status " + ad.getStatus() + ".");
        }

        ad.setStatus(AdvertisementStatus.DELETED);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Advertisement with id " + id + " not found.");
    }
}
