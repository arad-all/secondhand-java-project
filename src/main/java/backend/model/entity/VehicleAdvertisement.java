package backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Type-specific fields for ads in a vehicle category.
 */
@Getter
@Setter
@Entity
@Table(name = "vehicle_advertisements")
public class VehicleAdvertisement extends Advertisement {

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(length = 50)
    private String brand;
}
