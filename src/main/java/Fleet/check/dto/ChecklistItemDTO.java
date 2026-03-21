package Fleet.check.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDTO {
    private String itemCode;
    private String itemName;
    private String zoneName;
    private String response;
    private String remarks;
}
