package Fleet.check.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "checklist_items")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    /**
     * Response: YES, NO, NA (Not Applicable)
     */
    @Column(length = 10)
    private String response;

    @Column(length = 500)
    private String remarks;

    @Column(length = 1000)
    private String photoUrl;

    @Column(length = 1000)
    private String beforePhotoUrl;

    @Column(length = 1000)
    private String afterPhotoUrl;

    private boolean isFixed = false;

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
