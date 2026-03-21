package Fleet.check.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ShipmentDTO {
    private String shipmentId;
    private LocalDate dispatchDate;
    private String driverId;
    private String driverName;
    private String vehicleId;
    private Integer statusId;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
