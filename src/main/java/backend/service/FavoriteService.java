package backend.service;

import backend.controller.dto.FavoriteResponse;
import backend.exception.DuplicateResourceException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.FavoriteMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.Favorite;
import backend.model.entity.User;
import backend.repository.AdvertisementRepository;
import backend.repository.FavoriteRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Favorites — a user bookmarking an advertisement to find again later.
 * Every method is scoped to the caller's own {@code userId}; there's no
 * cross-user listing or removal by design.
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    /** The caller's saved ads. */
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(FavoriteMapper::toResponse)
                .toList();
    }

    /**
     * Saves an advertisement to the caller's favorites. Rejects an
     * already-favorited pair with {@link DuplicateResourceException}
     * (409) — the (user, advertisement) uniqueness this guards against
     * is also enforced at the DB level, but checking first avoids a
     * constraint-violation round trip. A non-existent advertisement
     * (or, in principle, user) is a plain {@link ResourceNotFoundException}.
     */
    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long advertisementId) {
        if (favoriteRepository.existsByUserIdAndAdvertisementId(userId, advertisementId)) {
            throw new DuplicateResourceException(
                    "Advertisement " + advertisementId + " is already in your favorites.");
        }

        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Advertisement with id " + advertisementId + " not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found."));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setAdvertisement(ad);
        favoriteRepository.save(favorite);

        return FavoriteMapper.toResponse(favorite);
    }

    /** Removes an advertisement from the caller's favorites. 404 if it wasn't favorited. */
    @Transactional
    public void removeFavorite(Long userId, Long advertisementId) {
        if (!favoriteRepository.existsByUserIdAndAdvertisementId(userId, advertisementId)) {
            throw new ResourceNotFoundException(
                    "Advertisement " + advertisementId + " is not in your favorites.");
        }
        favoriteRepository.deleteByUserIdAndAdvertisementId(userId, advertisementId);
    }
}
