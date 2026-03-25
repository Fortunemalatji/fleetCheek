package Fleet.check.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Data
public class ShipmentDTO {
    private String shipmentId;
    private LocalDate dispatchDate;
    private String location;
    private LocalTime shipmentTime;
    private String driverId;
    private String driverName;
    private String coDriverId;
    private String coDriverName;
    private String tripType;
    private Integer fleetGroupId;
    private String fleetGroupName;
    private String vehicleId;
    private String trailerId;
    private String trailerName;
    private Integer statusId;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonSetter("driver")
    public void unpackDriver(Map<String, String> driver) {
        if (driver != null) this.driverId = driver.get("userId");
    }

    @JsonSetter("vehicle")
    public void unpackVehicle(Map<String, String> vehicle) {
        if (vehicle != null) this.vehicleId = vehicle.get("vehicleId");
    }

    @JsonSetter("status")
    public void unpackStatus(Map<String, Object> status) {
        if (status != null) {
            Object id = status.get("id");
            if (id instanceof Integer) this.statusId = (Integer) id;
            else if (id instanceof String) this.statusId = Integer.parseInt((String) id);
        }
    }

    @JsonSetter("fleetGroup")
    public void unpackFleetGroup(Object fleetGroup) {
        if (fleetGroup instanceof String) {
            this.fleetGroupName = (String) fleetGroup;
        } else if (fleetGroup instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id instanceof Integer) this.fleetGroupId = (Integer) id;
            else if (id instanceof String) this.fleetGroupId = Integer.parseInt((String) id);

            Object name = map.get("name");
            if (name instanceof String) this.fleetGroupName = (String) name;
        }
    }
}
