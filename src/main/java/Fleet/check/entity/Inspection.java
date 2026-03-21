package Fleet.check.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inspections")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Inspection extends BaseEntity {
    @Id
    private UUID inspectionId = UUID.randomUUID();

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(length = 20)
    private String overallStatus; // PASS, FAIL, CONDITIONAL

    @Column(length = 100)
    private String gpsLocation;

    @Column(columnDefinition = "TEXT")
    private String driverSig;

    @Column(columnDefinition = "TEXT")
    private String supervisorSig;

    @Column(columnDefinition = "TEXT")
    private String securitySig;

    @Column(length = 20)
    private String signedByUserId;

    @OneToMany(mappedBy = "inspection", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistItem> checklistItems = new ArrayList<>();
}
