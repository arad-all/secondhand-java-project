package backend.service;

import backend.controller.dto.CreateRatingRequest;
import backend.controller.dto.RatingResponse;
import backend.controller.dto.SellerRatingsResponse;
import backend.exception.DuplicateResourceException;
import backend.exception.ForbiddenActionException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.model.entity.Advertisement;
import backend.model.entity.Rating;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementRepository;
import backend.repository.RatingRepository;
import backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RatingService ratingService;

    private User seller;
    private User buyer;
    private Advertisement soldAd;
    private Advertisement activeAd;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setUsername("seller");

        buyer = new User();
        buyer.setId(2L);
        buyer.setUsername("buyer");

        soldAd = new Advertisement();
        soldAd.setId(100L);
        soldAd.setTitle("Sold Item");
        soldAd.setStatus(AdvertisementStatus.SOLD);
        soldAd.setOwner(seller);
        soldAd.setBuyer(buyer);

        activeAd = new Advertisement();
        activeAd.setId(101L);
        activeAd.setTitle("Active Item");
        activeAd.setStatus(AdvertisementStatus.ACTIVE);
        activeAd.setOwner(seller);
        activeAd.setBuyer(null);
    }

    // ---- getSellerRatings ----

    @Test
    void getSellerRatings_returnsRatingsAndAverage() {
        when(userRepository.existsById(1L)).thenReturn(true);

        Rating rating = new Rating();
        rating.setId(10L);
        rating.setScore(5);
        rating.setComment("Great seller!");
        rating.setAdvertisement(soldAd);
        rating.setBuyer(buyer);
        rating.setSeller(seller);

        when(ratingRepository.findBySellerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(rating));
        when(ratingRepository.findAverageScoreBySellerId(1L)).thenReturn(5.0);
        when(ratingRepository.countBySellerId(1L)).thenReturn(1L);

        SellerRatingsResponse result = ratingService.getSellerRatings(1L);

        assertEquals(1L, result.sellerId());
        assertEquals(5.0, result.averageScore());
        assertEquals(1, result.totalRatings());
        assertEquals(1, result.ratings().size());
        assertEquals("Great seller!", result.ratings().get(0).comment());
    }

    @Test
    void getSellerRatings_returnsZeroAverage_whenNoRatings() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(ratingRepository.findBySellerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(ratingRepository.findAverageScoreBySellerId(1L)).thenReturn(null);
        when(ratingRepository.countBySellerId(1L)).thenReturn(0L);

        SellerRatingsResponse result = ratingService.getSellerRatings(1L);

        assertEquals(0.0, result.averageScore());
        assertEquals(0, result.totalRatings());
        assertTrue(result.ratings().isEmpty());
    }

    @Test
    void getSellerRatings_throws_whenSellerNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> ratingService.getSellerRatings(999L));
    }

    // ---- rate ----

    @Test
    void rate_happyPath() {
        CreateRatingRequest request = new CreateRatingRequest(5, "Perfect!");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(soldAd));
        when(ratingRepository.existsByAdvertisementIdAndBuyerId(100L, 2L)).thenReturn(false);

        RatingResponse result = ratingService.rate(100L, 2L, request);

        assertEquals(5, result.score());
        assertEquals("Perfect!", result.comment());
        assertEquals("buyer", result.buyerUsername());
        assertEquals("seller", result.sellerUsername());
        assertEquals(100L, result.advertisementId());
    }

    @Test
    void rate_throws_whenAdNotSold() {
        CreateRatingRequest request = new CreateRatingRequest(3, "Ok");
        when(advertisementRepository.findById(101L)).thenReturn(Optional.of(activeAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> ratingService.rate(101L, 2L, request));
    }

    @Test
    void rate_throws_whenCallerIsNotBuyer() {
        CreateRatingRequest request = new CreateRatingRequest(3, "Ok");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(soldAd));

        // User 99 is not the recorded buyer (buyer is user 2)
        assertThrows(ForbiddenActionException.class,
                () -> ratingService.rate(100L, 99L, request));
    }

    @Test
    void rate_throws_whenBuyerAlreadyRated() {
        CreateRatingRequest request = new CreateRatingRequest(4, "Good");
        when(advertisementRepository.findById(100L)).thenReturn(Optional.of(soldAd));
        when(ratingRepository.existsByAdvertisementIdAndBuyerId(100L, 2L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> ratingService.rate(100L, 2L, request));
    }

    @Test
    void rate_throws_whenAdNotFound() {
        CreateRatingRequest request = new CreateRatingRequest(3, "Meh");
        when(advertisementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ratingService.rate(999L, 2L, request));
    }
}
