package Fleet.check.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "vehicles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends BaseEntity {
    @Id
    @Column(length = 20)
    private String vehicleId; // e.g., TT3626

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private VehicleCategory category;

    @Column(nullable = false)
    private boolean isActive = true;

    private Integer lastOdometer;

}
