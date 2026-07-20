package backend.service;

import backend.controller.dto.CreateRatingRequest;
import backend.controller.dto.RatingResponse;
import backend.controller.dto.SellerRatingsResponse;
import backend.exception.DuplicateResourceException;
import backend.exception.ForbiddenActionException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.RatingMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.Rating;
import backend.model.entity.User;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementRepository;
import backend.repository.RatingRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ratings — a buyer rating (1-5, plus an optional comment) the seller of
 * an advertisement they bought. One rating per (advertisement, buyer),
 * enforced here and, as a safety net, by a DB unique constraint.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    /**
     * A seller's reputation: every rating they've received, plus the
     * average score and total count. {@code averageScore} is {@code 0.0}
     * when they have none yet, not {@code null}.
     */
    @Transactional(readOnly = true)
    public SellerRatingsResponse getSellerRatings(Long sellerId) {
        if (!userRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("User with id " + sellerId + " not found.");
        }

        var ratings = ratingRepository.findBySellerId(sellerId).stream()
                .map(RatingMapper::toResponse)
                .toList();
        Double averageScore = ratingRepository.findAverageScoreBySellerId(sellerId);
        long totalRatings = ratingRepository.countBySellerId(sellerId);

        return new SellerRatingsResponse(sellerId, averageScore != null ? averageScore : 0.0, totalRatings, ratings);
    }

    /**
     * Rates the seller of a SOLD advertisement.
     * <p>
     * The ad must be {@code SOLD} ({@link InvalidStateTransitionException}
     * otherwise), and the caller must be the buyer {@code markAsSold}
     * recorded for it — not just "anyone who isn't the seller"
     * ({@link ForbiddenActionException} otherwise). A given buyer can
     * only rate a given ad once ({@link DuplicateResourceException}).
     */
    @Transactional
    public RatingResponse rate(Long advertisementId, Long buyerId, CreateRatingRequest request) {
        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Advertisement with id " + advertisementId + " not found."));

        if (ad.getStatus() != AdvertisementStatus.SOLD) {
            throw new InvalidStateTransitionException("Cannot rate an advertisement that is not SOLD.");
        }
        if (ad.getBuyer() == null || !ad.getBuyer().getId().equals(buyerId)) {
            throw new ForbiddenActionException("Only the recorded buyer of this advertisement can rate it.");
        }
        if (ratingRepository.existsByAdvertisementIdAndBuyerId(advertisementId, buyerId)) {
            throw new DuplicateResourceException("You have already rated this advertisement.");
        }

        Rating rating = new Rating();
        rating.setAdvertisement(ad);
        rating.setBuyer(ad.getBuyer());
        rating.setSeller(ad.getOwner());
        rating.setScore(request.score());
        rating.setComment(request.comment());

        ratingRepository.save(rating);
        return RatingMapper.toResponse(rating);
    }
}
