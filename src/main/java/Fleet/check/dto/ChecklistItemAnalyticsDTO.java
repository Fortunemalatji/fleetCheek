package Fleet.check.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemAnalyticsDTO {
    private String itemCode;
    private String itemName;
    private String zoneName;
    private long totalDurationMs;
    private long averageDurationMs;
    private long maxDurationMs;
    private long submissions;
}
