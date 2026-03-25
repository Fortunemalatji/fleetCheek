package Fleet.check.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shipments")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Shipment extends BaseEntity {
    @Id
    @Column(length = 20)
    private String shipmentId; // e.g., 159494

    private LocalDate dispatchDate;

    @Column(length = 255)
    private String location;

    private LocalTime shipmentTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "co_driver_id")
    private User coDriver;

    @Column(length = 10)
    private String tripType; // SINGLE, DUO

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fleet_group_id")
    private FleetGroup fleetGroup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trailer_id")
    private Vehicle trailer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id")
    private ShipmentStatus status;
}
