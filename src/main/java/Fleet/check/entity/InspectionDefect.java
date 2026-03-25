package Fleet.check.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_defects")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDefect extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "checklist_item_id", nullable = false, unique = true)
    private ChecklistItem checklistItem;

    @Column(length = 20, nullable = false)
    private String status = "OPEN";

    @Column(length = 20, nullable = false)
    private String reportedByUserId;

    @Column(length = 20)
    private String resolvedByUserId;

    @Column(length = 1000, nullable = false)
    private String issuePhotoUrl;

    @Column(length = 1000)
    private String resolutionPhotoUrl;

    @Column(length = 500)
    private String issueRemarks;

    @Column(length = 500)
    private String resolutionRemarks;

    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}
