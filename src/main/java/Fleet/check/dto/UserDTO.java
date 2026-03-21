package Fleet.check.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private String userId;
    private String username;
    private String fullName;
    private String fleetGroup;
    private Integer roleId;
    private String roleName;
    private String pin; // Request only
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
