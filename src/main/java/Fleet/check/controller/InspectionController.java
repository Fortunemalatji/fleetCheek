package Fleet.check.controller;

import Fleet.check.dto.ChecklistItemDTO;
import Fleet.check.dto.ChecklistItemAnalyticsDTO;
import Fleet.check.dto.DefectResolutionDTO;
import Fleet.check.dto.InspectionDTO;
import Fleet.check.dto.InspectionDefectDTO;
import Fleet.check.entity.*;
import Fleet.check.repository.ChecklistTemplateRepository;
import Fleet.check.repository.InspectionRepository;
import Fleet.check.service.InspectionReportService;
import Fleet.check.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {
    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final InspectionReportService inspectionReportService;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    /** Get all inspections */
    @GetMapping
    public List<InspectionDTO> getAll() {
        return inspectionRepository.findAll().stream()
                .map(inspectionService::toInspectionDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<InspectionDTO> getByUserId(@PathVariable String userId) {
        return inspectionService.getByUserId(userId);
    }

    @GetMapping("/checklist/analytics")
    public List<ChecklistItemAnalyticsDTO> getChecklistAnalytics() {
        return inspectionReportService.getChecklistAnalytics();
    }

    @GetMapping("/checklist/report")
    public ResponseEntity<byte[]> exportChecklistReport(@RequestParam(defaultValue = "pdf") String format) {
        byte[] body = inspectionReportService.exportChecklistAnalyticsReport(format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=checklist-analytics." + format.toLowerCase())
                .contentType(resolveContentType(format))
                .body(body);
    }

    /**
     * STEP 1 - Start a new inspection.
     * Body: { "vehicle": { "vehicleId": "TT3626" }, "driver": { "userId": "0000827008" }, "tripType": "SINGLE" }
     */
    @PostMapping("/start")
    public InspectionDTO start(@RequestBody InspectionDTO inspection) {
        return inspectionService.startInspection(inspection);
    }

    /**
     * Get all available checklist items (use before submitting).
     * Optional filter: ?zone=ZONE_1_CAB | ZONE_2_FRONT | ZONE_3_SIDES | ZONE_4_REAR
     */
    @GetMapping("/checklist/templates")
    public List<ChecklistTemplate> getTemplates(@RequestParam(required = false) String zone) {
        return zone != null
                ? checklistTemplateRepository.findByZoneCode(zone)
                : checklistTemplateRepository.findAll();
    }

    /**
     * STEP 2 - Submit the full checklist in one request.
     * Body: [ { "itemCode": "GAUGES", "response": "YES", "remarks": "" }, ... ]
     */
    @PostMapping("/{id}/checklist")
    public List<ChecklistItemDTO> submitChecklist(
            @PathVariable UUID id,
            @RequestBody List<ChecklistItemDTO> items) {
        return inspectionService.submitChecklist(id, items);
    }

    @PostMapping("/{id}/checklist/{itemCode}/start")
    public ChecklistItemDTO startChecklistItem(@PathVariable UUID id,
                                               @PathVariable String itemCode) {
        return inspectionService.startChecklistItem(id, itemCode);
    }

    @PostMapping("/{id}/checklist/{itemCode}/complete")
    public ChecklistItemDTO completeChecklistItem(@PathVariable UUID id,
                                                  @PathVariable String itemCode,
                                                  @RequestBody ChecklistItemDTO item) {
        return inspectionService.completeChecklistItem(id, itemCode, item);
    }

    @PostMapping("/{id}/checklist/{itemCode}/resolve")
    public InspectionDefectDTO resolveChecklistDefect(@PathVariable UUID id,
                                                      @PathVariable String itemCode,
                                                      @RequestBody DefectResolutionDTO resolution) {
        return inspectionService.resolveDefect(id, itemCode, resolution);
    }

    /**
     * Follow up on a specific checklist item (e.g. mark a defect as FIXED).
     */
    @PatchMapping("/{id}/checklist/{itemCode}")
    public ChecklistItemDTO updateChecklistItem(
            @PathVariable UUID id,
            @PathVariable String itemCode,
            @RequestBody ChecklistItemDTO item) {
        return inspectionService.updateChecklistItem(id, itemCode, item);
    }

    /** Get all submitted checklist responses for an inspection. */
    @GetMapping("/{id}/checklist")
    public List<ChecklistItemDTO> getChecklist(@PathVariable UUID id) {
        return inspectionService.getChecklistResponse(id);
    }

    @GetMapping("/{id}/defects")
    public List<InspectionDefectDTO> getInspectionDefects(@PathVariable UUID id) {
        return inspectionService.getInspectionDefects(id);
    }

    @GetMapping("/defects/open")
    public List<InspectionDefectDTO> getOpenDefects() {
        return inspectionService.getOpenDefects();
    }

    /**
     * STEP 3 - End the inspection.
     * overall_status is auto-calculated: PASS / FAIL / CONDITIONAL (supervisor override on FAIL)
     */
    @PostMapping("/{id}/end")
    public InspectionDTO end(@PathVariable UUID id,
                           @RequestParam(required = false) String supervisorOverride,
                           @RequestParam(required = false) String driverSig,
                           @RequestParam(required = false) String supervisorSig,
                           @RequestParam(required = false) String securitySig,
                           @RequestParam(required = false) Integer endOdometer,
                           @RequestParam(required = false) String notes) {
        return inspectionService.endInspection(id, supervisorOverride, driverSig, supervisorSig, securitySig, endOdometer, notes);
    }

    @PostMapping("/{id}/cancel")
    public InspectionDTO cancel(@PathVariable UUID id,
                                @RequestParam(required = false) String reason) {
        return inspectionService.cancelInspection(id, reason);
    }

    @GetMapping("/{id}/verify")
    public InspectionDTO verify(@PathVariable UUID id) {
        return inspectionRepository.findById(id)
                .map(inspectionService::toInspectionDTO)
                .orElseThrow(() -> new RuntimeException("Inspection not found: " + id));
    }

    @GetMapping("/{id}/analytics")
    public java.util.Map<String, Long> getAnalytics(@PathVariable UUID id) {
        return inspectionService.getZoneAnalytics(id);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> exportInspectionReport(@PathVariable UUID id,
                                                         @RequestParam(defaultValue = "pdf") String format) {
        byte[] body = inspectionReportService.exportInspectionReport(id, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inspection-" + id + "." + format.toLowerCase())
                .contentType(resolveContentType(format))
                .body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inspectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveContentType(String format) {
        return switch (format.toLowerCase()) {
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv" -> MediaType.parseMediaType("text/csv");
            default -> MediaType.APPLICATION_PDF;
        };
    }
}
