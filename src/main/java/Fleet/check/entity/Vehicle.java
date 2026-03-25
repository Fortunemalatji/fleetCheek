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

    @Column(length = 20)
    private String registration; // e.g., ABC 123 GP

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String make;

    @Column(length = 50)
    private String model;

    private Integer year;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private VehicleCategory category;

    @Column(nullable = false)
    private boolean isActive = true;

    private Integer lastOdometer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fleet_group_id")
    private FleetGroup fleetGroup;

    @Column(length = 1000)
    private String photoUrl;
}
