package Fleet.check.service;

import Fleet.check.dto.ChecklistItemDTO;
import Fleet.check.dto.InspectionDTO;
import Fleet.check.entity.*;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionService {
    private final InspectionRepository inspectionRepository;
    private final ShipmentRepository shipmentRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;

    public InspectionDTO startInspection(InspectionDTO dto) {
        if (dto.getShipmentId() == null) {
            throw new ResourceNotFoundException("Shipment ID is required to start an inspection.");
        }
        Shipment shipment = shipmentRepository.findById(dto.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + dto.getShipmentId()));
        
        Inspection inspection = new Inspection();
        inspection.setShipment(shipment);
        inspection.setGpsLocation(dto.getGpsLocation());
        
        // Automatically link the vehicle from the shipment
        if (shipment.getVehicle() != null) {
            inspection.setVehicle(shipment.getVehicle());
        }

        inspection.setStartTime(LocalDateTime.now());
        return toInspectionDTO(inspectionRepository.save(inspection));
    }

    public List<ChecklistItemDTO> submitChecklist(UUID inspectionId, List<ChecklistItemDTO> requests) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + inspectionId));

        List<ChecklistItem> items = requests.stream().map(req -> {
            // Duplicate prevention
            if (checklistItemRepository.existsByInspection_InspectionIdAndTemplate_ItemCode(inspectionId, req.getItemCode())) {
                throw new IllegalStateException("Item " + req.getItemCode() + " already submitted for this inspection.");
            }

            ChecklistTemplate template = checklistTemplateRepository.findByItemCode(req.getItemCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Unknown checklist item: " + req.getItemCode()));
            
            ChecklistItem item = new ChecklistItem();
            item.setInspection(inspection);
            item.setTemplate(template);
            item.setResponse(req.getResponse());
            item.setRemarks(req.getRemarks());
            return item;
        }).collect(Collectors.toList());

        return checklistItemRepository.saveAll(items).stream()
                .map(this::toChecklistItemDTO)
                .collect(Collectors.toList());
    }

    public List<ChecklistItemDTO> getChecklistResponse(UUID inspectionId) {
        return checklistItemRepository.findByInspection_InspectionId(inspectionId).stream()
                .map(this::toChecklistItemDTO)
                .collect(Collectors.toList());
    }

    public InspectionDTO endInspection(UUID id, String supervisorOverride,
                                     String driverSig, String supervisorSig, String securitySig) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + id));

        // Completeness check
        long submittedCount = checklistItemRepository.countByInspection_InspectionId(id);
        long totalTemplates = checklistTemplateRepository.count();
        if (submittedCount < totalTemplates) {
            throw new IllegalStateException("Cannot end inspection: " + submittedCount + "/" + totalTemplates + " items submitted.");
        }

        List<ChecklistItem> items = checklistItemRepository.findByInspection_InspectionId(id);
        boolean hasCriticalFail = items.stream()
                .anyMatch(i -> i.getTemplate().isCritical() && "NO".equalsIgnoreCase(i.getResponse()));

        String status = hasCriticalFail
                ? ("CONDITIONAL".equalsIgnoreCase(supervisorOverride) ? "CONDITIONAL" : "FAIL")
                : "PASS";

        inspection.setEndTime(LocalDateTime.now());
        inspection.setOverallStatus(status);
        inspection.setDriverSig(driverSig);
        inspection.setSupervisorSig(supervisorSig);
        inspection.setSecuritySig(securitySig);
        
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        inspection.setSignedByUserId(currentUserId);
        
        return toInspectionDTO(inspectionRepository.save(inspection));
    }

    public InspectionDTO toInspectionDTO(Inspection inspection) {
        InspectionDTO dto = new InspectionDTO();
        dto.setId(inspection.getInspectionId());
        dto.setGpsLocation(inspection.getGpsLocation());
        dto.setOverallStatus(inspection.getOverallStatus());
        dto.setDriverSig(inspection.getDriverSig());
        dto.setSupervisorSig(inspection.getSupervisorSig());
        dto.setSecuritySig(inspection.getSecuritySig());
        dto.setSignedByUserId(inspection.getSignedByUserId());
        dto.setCreatedAt(inspection.getCreatedAt());
        dto.setUpdatedAt(inspection.getUpdatedAt());

        if (inspection.getShipment() != null) {
            dto.setShipmentId(inspection.getShipment().getShipmentId());
            if (inspection.getShipment().getDriver() != null) {
                dto.setDriverId(inspection.getShipment().getDriver().getUserId());
            }
        }
        
        if (inspection.getVehicle() != null) {
            dto.setVehicleId(inspection.getVehicle().getVehicleId());
        }

        if (inspection.getChecklistItems() != null) {
            dto.setChecklistItems(inspection.getChecklistItems().stream()
                    .map(this::toChecklistItemDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setChecklistItems(new ArrayList<>());
        }

        return dto;
    }

    private ChecklistItemDTO toChecklistItemDTO(ChecklistItem item) {
        ChecklistItemDTO dto = new ChecklistItemDTO();
        dto.setItemCode(item.getTemplate().getItemCode());
        dto.setItemName(item.getTemplate().getDisplayName());
        dto.setZoneName(item.getTemplate().getZoneName());
        dto.setResponse(item.getResponse());
        dto.setRemarks(item.getRemarks());
        return dto;
    }
}
