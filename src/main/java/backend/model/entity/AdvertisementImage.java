package backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One image belonging to an Advertisement. Storage of the actual file is
 * out of scope here — only the path/reference and display order live on
 * the entity.
 */
@Getter
@Setter
@Entity
@Table(name = "advertisement_images")
public class AdvertisementImage extends BaseEntity {

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;
}
