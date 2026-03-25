package Fleet.check.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class VehicleDTO {
    private String vehicleId;
    private String registration;
    private String name;
    private String make;
    private String model;
    private Integer year;
    private Integer categoryId;
    private String categoryName;
    private Double lastOdometer;
    private Boolean isActive;
    private Integer fleetGroupId;
    private String fleetGroupName;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonSetter("fleetGroup")
    public void unpackFleetGroup(Object fleetGroup) {
        if (fleetGroup instanceof String) {
            this.fleetGroupName = (String) fleetGroup;
        } else if (fleetGroup instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) fleetGroup;
            Object id = map.get("id");
            if (id instanceof Integer) this.fleetGroupId = (Integer) id;
            else if (id instanceof String) this.fleetGroupId = Integer.parseInt((String) id);

            Object name = map.get("name");
            if (name instanceof String) this.fleetGroupName = (String) name;
        }
    }

    @JsonSetter("category")
    public void unpackCategory(Map<String, Object> category) {
        if (category != null) {
            Object id = category.get("id");
            if (id instanceof Integer) this.categoryId = (Integer) id;
            else if (id instanceof String) this.categoryId = Integer.parseInt((String) id);
        }
    }
}
