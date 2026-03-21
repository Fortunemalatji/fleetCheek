package Fleet.check.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Lookup table for the 23 predefined vehicle inspection items.
 * Seeded once on startup by ChecklistDataSeeder.
 *
 * isCritical = true: a NO response auto-sets overall_status = FAIL
 * isHitch    = true: relate to securing of load/trailer
 */
@Entity
@Table(name = "checklist_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String zoneCode; // e.g. ZONE_1_CAB

    @Column(nullable = false, length = 60)
    private String zoneName; // e.g. "Zone 1: CAB (Interior & Controls)"

    @Column(nullable = false, unique = true, length = 40)
    private String itemCode; // e.g. GAUGES

    @Column(nullable = false, length = 100)
    private String displayName; // e.g. "Gauges"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isCritical;

    @Column(nullable = false)
    private boolean isHitch;

    @Column(nullable = false)
    private boolean isActive = true;
}
