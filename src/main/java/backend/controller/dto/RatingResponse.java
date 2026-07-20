package backend.controller.dto;

/** A single rating a buyer left for a seller on one advertisement. */
public record RatingResponse(
        Long id,
        Long advertisementId,
        String advertisementTitle,
        String buyerUsername,
        String sellerUsername,
        int score,
        String comment
) {
}
