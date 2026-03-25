package Fleet.check.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UserDTO {
    private String userId;
    private String username;
    private String fullName;
    private Integer fleetGroupId;
    private String fleetGroupName;
    private Integer roleId;
    private String roleName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pin; // Request only
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonSetter("role")
    public void unpackRole(Map<String, Object> role) {
        if (role != null) {
            Object id = role.get("id");
            if (id instanceof Integer) this.roleId = (Integer) id;
            else if (id instanceof String) this.roleId = Integer.parseInt((String) id);
        }
    }

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
}
