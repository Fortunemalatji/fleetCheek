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
    private String frontPhotoUrl;
    private String backPhotoUrl;
    private String leftPhotoUrl;
    private String rightPhotoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
