package Fleet.check.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDefectDTO {
    private Long id;
    private UUID inspectionId;
    private String itemCode;
    private String itemName;
    private String status;
    private String reportedByUserId;
    private String resolvedByUserId;
    private String issuePhotoUrl;
    private String resolutionPhotoUrl;
    private String issueRemarks;
    private String resolutionRemarks;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}
