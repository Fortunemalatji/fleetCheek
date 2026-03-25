package Fleet.check.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import java.util.Map;
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
    private String driverName;
    private String coDriverId;
    private String coDriverName;
    private String tripType;
    private String gpsLocation;
    private Integer fleetGroupId;
    private String fleetGroupName;
    private String overallStatus;
    private String driverSig;
    private String supervisorSig;
    private String securitySig;
    private String signedByUserId;
    private Integer startOdometer;
    private Integer endOdometer;
    private String notes;
    private String startedByUserId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalDurationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChecklistItemDTO> checklistItems;

    @JsonSetter("shipment")
    public void unpackShipment(Map<String, String> shipment) {
        if (shipment != null) this.shipmentId = shipment.get("shipmentId");
    }

    @JsonSetter("vehicle")
    public void unpackVehicle(Map<String, String> vehicle) {
        if (vehicle != null) this.vehicleId = vehicle.get("vehicleId");
    }

    @JsonSetter("driver")
    public void unpackDriver(Map<String, String> driver) {
        if (driver != null) this.driverId = driver.get("userId");
    }

    @JsonSetter("coDriver")
    public void unpackCoDriver(Map<String, String> coDriver) {
        if (coDriver != null) this.coDriverId = coDriver.get("userId");
    }
}
