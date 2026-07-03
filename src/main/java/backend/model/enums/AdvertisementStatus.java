package backend.model.enums;

/**
 * Lifecycle status of an Advertisement, from creation to sale/removal.
 */
public enum AdvertisementStatus {
    PENDING_REVIEW,
    ACTIVE,
    REJECTED,
    DELETED,
    SOLD
}
