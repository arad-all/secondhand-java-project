package backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Type-specific fields for ads in an electronics category.
 */
@Getter
@Setter
@Entity
@Table(name = "electronics_advertisements")
public class ElectronicsAdvertisement extends Advertisement {

    @Column(length = 50)
    private String brand;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;
}
