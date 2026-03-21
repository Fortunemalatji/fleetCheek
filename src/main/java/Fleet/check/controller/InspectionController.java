package Fleet.check.controller;

import Fleet.check.dto.ChecklistItemDTO;
import Fleet.check.dto.InspectionDTO;
import Fleet.check.entity.*;
import Fleet.check.repository.ChecklistTemplateRepository;
import Fleet.check.repository.InspectionRepository;
import Fleet.check.service.InspectionService;
import lombok.RequiredArgsConstructor;
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
    private final ChecklistTemplateRepository checklistTemplateRepository;

    /** Get all inspections */
    @GetMapping
    public List<InspectionDTO> getAll() {
        return inspectionRepository.findAll().stream()
                .map(inspectionService::toInspectionDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * STEP 1 - Start a new inspection.
     * Body: { "shipment": { "shipmentId": "159494" }, "gpsLocation": "-26.1, 28.4" }
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

    /** Get all submitted checklist responses for an inspection. */
    @GetMapping("/{id}/checklist")
    public List<ChecklistItemDTO> getChecklist(@PathVariable UUID id) {
        return inspectionService.getChecklistResponse(id);
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
                           @RequestParam(required = false) String securitySig) {
        return inspectionService.endInspection(id, supervisorOverride, driverSig, supervisorSig, securitySig);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inspectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
