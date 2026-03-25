package Fleet.check.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDTO {
    private Long id;
    private String itemCode;
    private String itemName;
    private String zoneName;
    private String status;
    private boolean critical;
    private String response;
    private String remarks;
    private String photoUrl;
    private String beforePhotoUrl;
    private String afterPhotoUrl;
    private boolean isFixed;
    private String defectStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
}
