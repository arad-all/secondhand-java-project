package backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * A buyer's rating (1-5) of a seller for a specific advertisement.
 * The unique constraint on (advertisement, buyer) enforces one rating
 * per buyer per ad at the database level, as a safety net alongside
 * the service-layer check.
 */
@Getter
@Setter
@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rating_ad_buyer",
                columnNames = {"advertisement_id", "buyer_id"}))
public class Rating extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int score;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
