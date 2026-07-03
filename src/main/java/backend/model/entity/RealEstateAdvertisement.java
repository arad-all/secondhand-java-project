package backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Type-specific fields for ads in a real-estate category.
 */
@Getter
@Setter
@Entity
@Table(name = "real_estate_advertisements")
public class RealEstateAdvertisement extends Advertisement {

    @Column(name = "area_sqm")
    private Double areaSqm;

    private Integer rooms;
}
