package backend.service;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.CreateAdvertisementRequest;
import backend.controller.dto.UpdateAdvertisementRequest;
import backend.exception.ForbiddenActionException;
import backend.exception.InvalidFileException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.AdvertisementMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.AdvertisementImage;
import backend.model.entity.Category;
import backend.model.entity.City;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementImageRepository;
import backend.repository.AdvertisementRepository;
import backend.repository.CategoryRepository;
import backend.repository.CityRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    /** Upper bound on images per advertisement, to keep uploads bounded. */
    private static final int MAX_IMAGES_PER_ADVERTISEMENT = 8;

    private final AdvertisementRepository advertisementRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final AdvertisementImageRepository advertisementImageRepository;
    private final FileStorageService fileStorageService;

    /**
     * Keyword/filter browse-and-search. Unlike {@link #getActiveAdvertisements},
     * this supports narrowing by category, city, price range, and a
     * free-text keyword (matched against title/description) on top of the
     * required {@code status}.
     * <p>
     * Only an {@code ADMIN} caller may search anything other than
     * {@code ACTIVE} — anyone else's requested {@code status} is silently
     * overridden back to {@code ACTIVE}, the same visibility rule
     * {@link #getById} enforces for the single-ad view. {@code isAdmin} is
     * resolved by the controller from the JWT and passed in as a plain
     * boolean; this method is what actually decides what that identity is
     * allowed to do with it.
     * <p>
     * Same reasoning applies to the owner's account status: a non-admin
     * caller only ever sees ads owned by an {@code ACTIVE} account, so a
     * blocked user's ads disappear from public search immediately, even
     * though the ads themselves are still {@code ACTIVE}. An admin's
     * search isn't filtered this way, since moderation may need to find
     * an ad regardless of its owner's current standing.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> search(String keyword,
                                                     Long categoryId,
                                                     Long cityId,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice,
                                                     AdvertisementStatus status,
                                                     boolean isAdmin,
                                                     Pageable pageable) {
        AdvertisementStatus effectiveStatus = isAdmin ? status : AdvertisementStatus.ACTIVE;
        AccountStatus ownerStatus = isAdmin ? null : AccountStatus.ACTIVE;
        List<Long> categoryIds = (categoryId != null)
                ? categoryRepository.findIdsIncludingDescendants(categoryId)
                : null;
        Page<Advertisement> results = advertisementRepository.search(
                effectiveStatus, ownerStatus, categoryIds, cityId, minPrice, maxPrice, keyword, pageable);
        return results.map(AdvertisementMapper::toSummary);
    }

    /**
     * Plain public listing of every {@code ACTIVE} advertisement — what
     * {@code GET /api/advertisements} returns today. Backed directly by
     * {@link AdvertisementRepository#findByStatus} rather than the
     * filtered {@link #search}: once a caller needs keyword/category/
     * city/price filtering, {@code search} is the method for that.
     * <p>
     * Returns a {@link Page}, like every other list-returning method
     * here, even though the controller currently unwraps it into a flat
     * list (the JavaFX client doesn't send or understand pagination yet)
     * — so the moment that's wired up, this method needs no changes.
     * <p>
     * Also requires the owner's account to be {@code ACTIVE}: this is a
     * fully public, unauthenticated endpoint, so a blocked user's ads must
     * never appear here, same as {@link #search} enforces for its own
     * non-admin callers.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> getActiveAdvertisements(Pageable pageable) {
        return advertisementRepository.findByStatusAndOwnerStatus(AdvertisementStatus.ACTIVE, AccountStatus.ACTIVE, pageable)
                .map(AdvertisementMapper::toSummary);
    }

    /**
     * Fetches one advertisement's detail view. An {@code ACTIVE} ad is
     * visible to anyone; anything else (pending review, rejected, sold,
     * deleted) is visible only to its owner, its buyer (once one is
     * recorded — see {@link #markAsSold}), or an admin — a buyer must
     * still be able to open the ad they purchased from their purchase
     * history after it's gone SOLD. An ad owned by a {@code BLOCKED}
     * account is treated the same way — hidden from everyone except the
     * owner/buyer (moot in practice, since a blocked account's requests
     * are rejected before they ever reach this method; see
     * {@code JwtAuthenticationFilter}) or an admin. Any other caller
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
        boolean isBuyer = currentUserId != null && ad.getBuyer() != null && ad.getBuyer().getId().equals(currentUserId);
        boolean ownerBlocked = ad.getOwner().getStatus() == AccountStatus.BLOCKED;
        if ((ad.getStatus() != AdvertisementStatus.ACTIVE || ownerBlocked) && !isOwner && !isBuyer && !isAdmin) {
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
     * Advertisements the caller purchased. The buyer relationship is set
     * only by {@link #markAsSold} when an ACTIVE advertisement transitions
     * to SOLD, so this list is the caller's purchase history rather than
     * advertisements they merely messaged or favorited.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> getPurchasedAdvertisements(Long buyerId, Pageable pageable) {
        return advertisementRepository.findByBuyerId(buyerId, pageable)
                .map(AdvertisementMapper::toSummary);
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
     * All advertisements awaiting moderation (plan §"Moderation"). Backs
     * the admin "review queue" view — enforcement of who may call this
     * (ADMIN only) lives in {@code SecurityConfig}'s {@code /api/admin/**}
     * rule, not here.
     */
    @Transactional(readOnly = true)
    public Page<AdvertisementSummaryResponse> listPendingReview(Pageable pageable) {
        return advertisementRepository.findByStatus(AdvertisementStatus.PENDING_REVIEW, pageable)
                .map(AdvertisementMapper::toSummary);
    }

    /**
     * Approves a pending advertisement, publishing it. Only a valid
     * transition from {@code PENDING_REVIEW} — approving an already-
     * ACTIVE, REJECTED, SOLD, or DELETED ad is rejected. Clears any
     * previous {@code adminNote} (e.g. left by an earlier rejection that
     * was reconsidered) since it no longer applies once approved.
     */
    @Transactional
    public AdvertisementDetailResponse approve(Long adId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        if (ad.getStatus() != AdvertisementStatus.PENDING_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot approve advertisement from status " + ad.getStatus() + ".");
        }

        ad.setStatus(AdvertisementStatus.ACTIVE);
        ad.setAdminNote(null);
        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * Rejects a pending advertisement, recording why via {@code adminNote}
     * so the owner can see the reason. Only a valid transition from
     * {@code PENDING_REVIEW}, same as {@link #approve}.
     */
    @Transactional
    public AdvertisementDetailResponse reject(Long adId, String reason) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        if (ad.getStatus() != AdvertisementStatus.PENDING_REVIEW) {
            throw new InvalidStateTransitionException(
                    "Cannot reject advertisement from status " + ad.getStatus() + ".");
        }

        ad.setStatus(AdvertisementStatus.REJECTED);
        ad.setAdminNote(reason);
        return AdvertisementMapper.toDetail(ad);
    }


    /**
     * Marks the ad sold. Owner-only, and only a valid transition from
     * {@code ACTIVE} — trying to sell a PENDING_REVIEW/REJECTED/DELETED/
     * already-SOLD ad is rejected, not silently accepted. Also records
     * {@code buyerId} as the ad's buyer — this becomes the one fact
     * {@code RatingService#rate} trusts to confirm who's allowed to rate
     * the seller, so it's validated here, not taken on faith: the buyer
     * must be a real user, and can't be the owner themselves.
     */
    @Transactional
    public AdvertisementDetailResponse markAsSold(Long adId, Long ownerId, Long buyerId) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        if (!ad.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenActionException("Only the ad's owner can mark it as sold.");
        }
        if (ad.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Cannot mark advertisement as SOLD from status " + ad.getStatus() + ".");
        }
        if (buyerId.equals(ownerId)) {
            throw new ForbiddenActionException("The owner cannot be recorded as the buyer of their own advertisement.");
        }
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + buyerId + " not found."));

        ad.setBuyer(buyer);
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

    /**
     * Edits an advertisement's title/description/price/category/city.
     * Owner or admin only ({@link ForbiddenActionException} otherwise),
     * and only while the advertisement is {@link AdvertisementStatus#ACTIVE}
     * ({@link InvalidStateTransitionException} otherwise). Partial update:
     * only non-null fields in {@code request} are applied.
     */
    @Transactional
    public AdvertisementDetailResponse editAdvertisement(Long adId,
                                                         UpdateAdvertisementRequest request,
                                                         Long currentUserId,
                                                         boolean isAdmin) {
        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        boolean isOwner = ad.getOwner().getId().equals(currentUserId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenActionException("Only the ad's owner or an admin can edit this advertisement.");
        }
        if (ad.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Cannot edit advertisement from status " + ad.getStatus() + ".");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category with id " + request.categoryId() + " not found."));
        }
        City city = null;
        if (request.cityId() != null) {
            city = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "City with id " + request.cityId() + " not found."));
        }

        if (request.title() != null) {
            ad.setTitle(request.title());
        }
        if (request.description() != null) {
            ad.setDescription(request.description());
        }
        if (request.price() != null) {
            ad.setPrice(request.price());
        }
        if (category != null) {
            ad.setCategory(category);
        }
        if (city != null) {
            ad.setCity(city);
        }

        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * Adds one or more images to an advertisement. Owner-only (unlike
     * {@link #editAdvertisement} / {@link #deleteAdvertisement}, an admin
     * cannot add images to someone else's ad — there's no legitimate
     * reason for an admin to be supplying photos of another user's item),
     * and only while the ad is in one of {@link #DELETABLE_STATUSES};
     * adding photos to an already-{@code SOLD}/{@code DELETED} listing
     * makes no sense.
     * <p>
     * Files are validated and written to disk one at a time via
     * {@link FileStorageService#store}; if any file in the batch fails
     * (bad type, empty, I/O error), every file already stored earlier in
     * this same call is deleted again before the exception propagates, so
     * a partially-failed upload doesn't leave orphaned files on disk.
     * New images are appended after any existing ones, continuing the
     * {@code displayOrder} sequence rather than restarting it.
     */
    @Transactional
    public AdvertisementDetailResponse addImages(Long adId, List<MultipartFile> files, Long currentUserId) {
        if (files == null || files.isEmpty()) {
            throw new InvalidFileException("At least one image file is required.");
        }

        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> notFound(adId));

        if (!ad.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenActionException("Only the ad's owner can add images to this advertisement.");
        }
        if (!DELETABLE_STATUSES.contains(ad.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Cannot add images to advertisement from status " + ad.getStatus() + ".");
        }

        long existingCount = advertisementImageRepository.countByAdvertisementId(adId);
        if (existingCount + files.size() > MAX_IMAGES_PER_ADVERTISEMENT) {
            throw new InvalidFileException(
                    "This advertisement already has " + existingCount + " image(s); at most "
                            + MAX_IMAGES_PER_ADVERTISEMENT + " are allowed in total.");
        }

        List<String> storedFilenames = new ArrayList<>();
        int displayOrder = (int) existingCount;
        try {
            for (MultipartFile file : files) {
                String filename = fileStorageService.store(adId, file);
                storedFilenames.add(filename);

                AdvertisementImage image = new AdvertisementImage();
                image.setAdvertisement(ad);
                image.setImagePath(filename);
                image.setDisplayOrder(displayOrder++);
                ad.getImages().add(image);
            }
        } catch (RuntimeException e) {
            storedFilenames.forEach(filename -> fileStorageService.delete(adId, filename));
            throw e;
        }

        return AdvertisementMapper.toDetail(ad);
    }

    /**
     * Resolves a stored image back to a readable {@link Resource} for the
     * download endpoint. Confirms the filename is actually recorded
     * against this advertisement (not just present somewhere on disk)
     * before touching the filesystem, so a stale or mismatched filename
     * gets the same 404 as one that never existed.
     */
    @Transactional(readOnly = true)
    public Resource loadImage(Long adId, String filename) {
        advertisementImageRepository.findByAdvertisementIdAndImagePath(adId, filename)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found."));
        return fileStorageService.load(adId, filename);
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Advertisement with id " + id + " not found.");
    }
}
