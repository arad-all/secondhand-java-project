package backend.controller.dto;

/**
 * A saved advertisement. {@code id} is the favorite row's own id (unused
 * by the client today, but there if it's ever needed); the advertisement
 * itself is a summary, same shape as the browse/list endpoints use.
 */
public record FavoriteResponse(
        Long id,
        AdvertisementSummaryResponse advertisement
) {
}
