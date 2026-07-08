package backend.controller;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.CreateAdvertisementRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST endpoints for advertisements.
 *
 * IMPORTANT: This controller intentionally contains no business logic,
 * no repository access and no database queries. Those belong in a future
 * AdvertisementService, once the team member responsible for backend
 * services implements it. Every method below returns placeholder/sample
 * data so the frontend can be built and tested against a stable contract
 * in the meantime.
 */
@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementController {

    @GetMapping
    public List<AdvertisementSummaryResponse> getActiveAdvertisements() {
        // TODO: delegate to an AdvertisementService (not implemented yet) that should:
        //   1. Return only advertisements with status = ACTIVE.
        //   2. Support keyword search and filters (category, city, price range)
        //      via request parameters once that part of the spec is implemented.
        return List.of(
                new AdvertisementSummaryResponse(1L, "Sample Laptop", new BigDecimal("15000000"),
                        "Tehran", "Electronics", "ACTIVE"),
                new AdvertisementSummaryResponse(2L, "Sample Bicycle", new BigDecimal("3000000"),
                        "Isfahan", "Sports", "ACTIVE")
        );
    }

    @GetMapping("/{id}")
    public AdvertisementDetailResponse getAdvertisementById(@PathVariable Long id) {
        // TODO: delegate to an AdvertisementService (not implemented yet) that should:
        //   1. Fetch the advertisement by id.
        //   2. Return 404 if it doesn't exist or isn't visible to the caller
        //      (e.g. PENDING_REVIEW ads should only be visible to their owner/admin).
        return new AdvertisementDetailResponse(
                id,
                "Sample Laptop",
                "Used laptop in good condition.",
                new BigDecimal("15000000"),
                "Tehran",
                "Electronics",
                "ACTIVE",
                "sampleSeller",
                List.of());
    }

    @PostMapping
    public AdvertisementDetailResponse createAdvertisement(
            @Valid @RequestBody CreateAdvertisementRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        // TODO: delegate to an AdvertisementService (not implemented yet) that should:
        //   1. Resolve the current user from the JWT in the Authorization header
        //      (never trust an ownerId sent by the client).
        //   2. Validate that categoryId and cityId refer to existing records.
        //   3. Save the new advertisement with status = PENDING_REVIEW.
        //   4. Persist any uploaded images and link them to the advertisement.
        // For now we simply echo back the submitted data with a PENDING_REVIEW
        // status so the frontend team can build and test this flow end-to-end.
        return new AdvertisementDetailResponse(
                null,
                request.title(),
                request.description(),
                request.price(),
                "City#" + request.cityId(),
                "Category#" + request.categoryId(),
                "PENDING_REVIEW",
                "currentUser",
                List.of());
    }
}
