package backend.model.entity;

import backend.model.enums.AdvertisementStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The core listing entity. Kept non-abstract so plain/generic categories
 * don't need a subclass — only categories that need extra fields get one
 * (see VehicleAdvertisement, ElectronicsAdvertisement, RealEstateAdvertisement).
 * Uses JOINED inheritance: each subtype gets its own table with only its
 * own extra columns, joined back to this one by id — no nullable columns
 * for fields that don't apply to a given type.
 */
@Getter
@Setter
@Entity
@Table(name = "advertisements")
@Inheritance(strategy = InheritanceType.JOINED)
public class Advertisement extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Current average rating of this advertisement's seller. This is a
     * read-only derived value rather than duplicated state, so existing and
     * newly submitted ratings are reflected immediately and can be used by
     * Spring Data's database-level sorting before pagination is applied.
     * Sellers without ratings sort as rating 0.
     */
    @Formula("(SELECT COALESCE(AVG(r.score), 0.0) FROM ratings r WHERE r.seller_id = owner_id)")
    private Double sellerRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvertisementStatus status = AdvertisementStatus.PENDING_REVIEW;

    /** Set by an admin, typically explaining why the ad was rejected. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Who bought this ad — {@code null} until {@code markAsSold} sets it,
     * and never set otherwise. This is the source of truth for "who is
     * allowed to rate this ad's seller" (see {@code RatingService#rate}),
     * not just a display detail.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdvertisementImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conversation> conversations = new ArrayList<>();

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favoritedBy = new ArrayList<>();

    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings = new ArrayList<>();
}
