package Fleet.check.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {
    private String vehicleId;
    private Integer categoryId;
    private String categoryName;
    private Double lastOdometer;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
