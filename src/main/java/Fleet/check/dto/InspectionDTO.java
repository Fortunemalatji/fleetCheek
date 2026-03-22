package Fleet.check.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class InspectionDTO {
    private UUID id;
    private String shipmentId;
    private String vehicleId;
    private String trailerId;
    private String trailerName;
    private String driverId;
    private String gpsLocation;
    private String overallStatus;
    private String driverSig;
    private String supervisorSig;
    private String securitySig;
    private String signedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChecklistItemDTO> checklistItems;
}
