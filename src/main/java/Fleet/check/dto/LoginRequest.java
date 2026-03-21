package Fleet.check.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String userId;
    private String pin;
}
