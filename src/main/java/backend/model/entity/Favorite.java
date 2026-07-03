package backend.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A saved/bookmarked advertisement for a user. The unique constraint
 * on (user, advertisement) prevents saving the same ad twice.
 */
@Getter
@Setter
@Entity
@Table(name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_ad",
                columnNames = {"user_id", "advertisement_id"}))
public class Favorite extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;
}
