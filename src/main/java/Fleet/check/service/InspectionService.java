package Fleet.check.service;

import Fleet.check.dto.ChecklistItemDTO;
import Fleet.check.dto.DefectResolutionDTO;
import Fleet.check.dto.InspectionDTO;
import Fleet.check.dto.InspectionDefectDTO;
import Fleet.check.entity.*;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final InspectionDefectRepository inspectionDefectRepository;
    private final ShipmentStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public InspectionDTO startInspection(InspectionDTO dto) {
        Inspection inspection = new Inspection();
        inspection.setGpsLocation(dto.getGpsLocation());
        inspection.setStartOdometer(dto.getStartOdometer());
        inspection.setNotes(dto.getNotes());
        inspection.setTripType(dto.getTripType());

        Shipment shipment = null;
        if (dto.getShipmentId() != null && !dto.getShipmentId().isBlank()) {
            shipment = shipmentRepository.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + dto.getShipmentId()));
            inspection.setShipment(shipment);
        }

        if (shipment != null) {
            if (shipment.getVehicle() != null) {
                inspection.setVehicle(shipment.getVehicle());
            }
            if (shipment.getTrailer() != null) {
                inspection.setTrailer(shipment.getTrailer());
            }
            if (shipment.getFleetGroup() != null) {
                inspection.setFleetGroup(shipment.getFleetGroup());
            }
            if (shipment.getDriver() != null) {
                inspection.setDriver(shipment.getDriver());
            }
            if (shipment.getCoDriver() != null) {
                inspection.setCoDriver(shipment.getCoDriver());
            }
            if (inspection.getTripType() == null) {
                inspection.setTripType(shipment.getTripType());
            }
        } else {
            if (dto.getVehicleId() == null || dto.getVehicleId().isBlank()) {
                throw new ResourceNotFoundException("Vehicle ID is required to start an inspection.");
            }
            if (dto.getDriverId() == null || dto.getDriverId().isBlank()) {
                throw new ResourceNotFoundException("Driver ID is required to start an inspection.");
            }

            Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + dto.getVehicleId()));
            inspection.setVehicle(vehicle);

            if (dto.getTrailerId() != null && !dto.getTrailerId().isBlank()) {
                inspection.setTrailer(vehicleRepository.findById(dto.getTrailerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Trailer not found: " + dto.getTrailerId())));
            }

            User driver = userRepository.findById(dto.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + dto.getDriverId()));
            inspection.setDriver(driver);

            if (dto.getCoDriverId() != null && !dto.getCoDriverId().isBlank()) {
                User coDriver = userRepository.findById(dto.getCoDriverId())
                        .orElseThrow(() -> new ResourceNotFoundException("Co-driver not found: " + dto.getCoDriverId()));
                inspection.setCoDriver(coDriver);
            }

            if (vehicle.getFleetGroup() != null) {
                inspection.setFleetGroup(vehicle.getFleetGroup());
            }
        }

        validateTripType(inspection);

        inspection.setStartTime(LocalDateTime.now());
        
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        inspection.setStartedByUserId(currentUserId);

        // Automate Shipment Status -> IN_PROGRESS
        if (shipment != null) {
            statusRepository.findByName("IN_PROGRESS").ifPresent(shipment::setStatus);
            shipmentRepository.save(shipment);
        }

        Inspection savedInspection = inspectionRepository.save(inspection);
        createInspectionItems(savedInspection);

        return toInspectionDTO(inspectionRepository.findById(savedInspection.getInspectionId()).orElse(savedInspection));
    }

    public List<ChecklistItemDTO> submitChecklist(UUID inspectionId, List<ChecklistItemDTO> requests) {
        return requests.stream()
                .map(req -> completeChecklistItem(inspectionId, req.getItemCode(), req))
                .collect(Collectors.toList());
    }

    public ChecklistItemDTO updateChecklistItem(UUID inspectionId, String itemCode, ChecklistItemDTO req) {
        ChecklistItem item = checklistItemRepository.findByInspection_InspectionIdAndTemplate_ItemCode(inspectionId, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item " + itemCode + " not found for inspection " + inspectionId));
        
        if (req.getResponse() != null) item.setResponse(req.getResponse());
        if (req.getRemarks() != null) item.setRemarks(req.getRemarks());
        if (req.getPhotoUrl() != null) item.setPhotoUrl(req.getPhotoUrl());
        if (req.getBeforePhotoUrl() != null) item.setBeforePhotoUrl(req.getBeforePhotoUrl());
        if (req.getAfterPhotoUrl() != null) item.setAfterPhotoUrl(req.getAfterPhotoUrl());
        item.setFixed(req.isFixed()); // boolean
        if (req.getStatus() != null) item.setStatus(req.getStatus());
        if (req.getStartTime() != null) item.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) item.setEndTime(req.getEndTime());
        
        return toChecklistItemDTO(checklistItemRepository.save(item));
    }

    public List<ChecklistItemDTO> getChecklistResponse(UUID inspectionId) {
        return checklistItemRepository.findByInspection_InspectionIdOrderById(inspectionId).stream()
                .map(this::toChecklistItemDTO)
                .collect(Collectors.toList());
    }

    public ChecklistItemDTO startChecklistItem(UUID inspectionId, String itemCode) {
        Inspection inspection = getInspection(inspectionId);
        ChecklistItem item = getChecklistItem(inspectionId, itemCode);

        if (inspection.getEndTime() != null) {
            throw new IllegalStateException("Cannot start items for a completed inspection.");
        }
        if (checklistItemRepository.existsByInspection_InspectionIdAndStatus(inspectionId, "WAITING_FOR_RESOLUTION")) {
            throw new IllegalStateException("Resolve the open critical defect before continuing.");
        }
        if (checklistItemRepository.existsByInspection_InspectionIdAndStatus(inspectionId, "IN_PROGRESS")
                && !"IN_PROGRESS".equals(item.getStatus())) {
            throw new IllegalStateException("Another checklist item is already in progress.");
        }

        if ("COMPLETED".equals(item.getStatus())) {
            return toChecklistItemDTO(item);
        }

        if (item.getStartTime() == null) {
            item.setStartTime(LocalDateTime.now());
        }
        item.setStatus("IN_PROGRESS");
        return toChecklistItemDTO(checklistItemRepository.save(item));
    }

    public ChecklistItemDTO completeChecklistItem(UUID inspectionId, String itemCode, ChecklistItemDTO req) {
        ChecklistItem item = getChecklistItem(inspectionId, itemCode);

        if (item.getStartTime() == null) {
            item.setStartTime(req.getStartTime() != null ? req.getStartTime() : LocalDateTime.now());
        }

        if (req.getResponse() == null || req.getResponse().isBlank()) {
            throw new IllegalStateException("Checklist response is required.");
        }

        item.setResponse(req.getResponse());
        item.setRemarks(req.getRemarks());
        item.setPhotoUrl(req.getPhotoUrl());
        item.setBeforePhotoUrl(req.getBeforePhotoUrl());
        item.setAfterPhotoUrl(req.getAfterPhotoUrl());
        item.setEndTime(req.getEndTime() != null ? req.getEndTime() : LocalDateTime.now());

        if (item.getTemplate().isCritical() && "NO".equalsIgnoreCase(req.getResponse())) {
            String evidencePhoto = firstNonBlank(req.getBeforePhotoUrl(), req.getPhotoUrl());
            if (evidencePhoto == null) {
                throw new IllegalStateException("Critical failed items require evidence photo before proceeding.");
            }

            item.setBeforePhotoUrl(evidencePhoto);
            item.setStatus("WAITING_FOR_RESOLUTION");
            item.setFixed(false);
            ChecklistItem savedItem = checklistItemRepository.save(item);
            openOrRefreshDefect(savedItem, req.getRemarks(), evidencePhoto);
            return toChecklistItemDTO(savedItem);
        }

        item.setStatus("COMPLETED");
        item.setFixed(req.isFixed());
        return toChecklistItemDTO(checklistItemRepository.save(item));
    }

    public InspectionDefectDTO resolveDefect(UUID inspectionId, String itemCode, DefectResolutionDTO req) {
        if (req.getResolutionPhotoUrl() == null || req.getResolutionPhotoUrl().isBlank()) {
            throw new IllegalStateException("Resolution photo is required to resolve a defect.");
        }

        ChecklistItem item = getChecklistItem(inspectionId, itemCode);
        InspectionDefect defect = inspectionDefectRepository.findByChecklistItem_Id(item.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Open defect not found for item " + itemCode));

        defect.setStatus("RESOLVED");
        defect.setResolutionPhotoUrl(req.getResolutionPhotoUrl());
        defect.setResolutionRemarks(req.getResolutionRemarks());
        defect.setResolvedAt(LocalDateTime.now());
        defect.setResolvedByUserId(currentUserId());

        item.setAfterPhotoUrl(req.getResolutionPhotoUrl());
        item.setFixed(true);
        item.setStatus("COMPLETED");

        inspectionDefectRepository.save(defect);
        checklistItemRepository.save(item);
        return toDefectDTO(defect);
    }

    public List<InspectionDefectDTO> getOpenDefects() {
        return inspectionDefectRepository.findByStatusOrderByReportedAtDesc("OPEN").stream()
                .map(this::toDefectDTO)
                .collect(Collectors.toList());
    }

    public List<InspectionDefectDTO> getInspectionDefects(UUID inspectionId) {
        getInspection(inspectionId);
        return inspectionDefectRepository.findByInspection_InspectionIdOrderByReportedAtAsc(inspectionId).stream()
                .map(this::toDefectDTO)
                .collect(Collectors.toList());
    }

    public List<InspectionDTO> getByUserId(String userId) {
        return inspectionRepository
                .findDistinctByShipment_Driver_UserIdOrShipment_CoDriver_UserIdOrDriver_UserIdOrCoDriver_UserIdOrStartedByUserIdOrSignedByUserId(
                        userId, userId, userId, userId, userId, userId
                ).stream()
                .map(this::toInspectionDTO)
                .collect(Collectors.toList());
    }

    public InspectionDTO endInspection(UUID id, String supervisorOverride,
                                     String driverSig, String supervisorSig, String securitySig,
                                     Integer endOdometer, String notes) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + id));

        // Completeness check
        long submittedCount = checklistItemRepository.countByInspection_InspectionId(id);
        long totalTemplates = checklistTemplateRepository.count();
        if (submittedCount < totalTemplates) {
            throw new IllegalStateException("Cannot end inspection: " + submittedCount + "/" + totalTemplates + " items submitted.");
        }
        long completedCount = checklistItemRepository.countByInspection_InspectionIdAndStatus(id, "COMPLETED");
        if (completedCount < totalTemplates) {
            throw new IllegalStateException("Cannot end inspection: " + completedCount + "/" + totalTemplates + " checklist items are completed.");
        }
        if (inspectionDefectRepository.findByInspection_InspectionIdOrderByReportedAtAsc(id).stream()
                .anyMatch(defect -> "OPEN".equals(defect.getStatus()))) {
            throw new IllegalStateException("Cannot end inspection while critical defects are still open.");
        }

        List<ChecklistItem> items = checklistItemRepository.findByInspection_InspectionIdOrderById(id);
        boolean hasCriticalFail = items.stream()
                .anyMatch(i -> i.getTemplate().isCritical() && "NO".equalsIgnoreCase(i.getResponse()) && !i.isFixed());

        String status = hasCriticalFail
                ? ("CONDITIONAL".equalsIgnoreCase(supervisorOverride) ? "CONDITIONAL" : "FAIL")
                : "PASS";

        inspection.setEndTime(LocalDateTime.now());
        inspection.setOverallStatus(status);
        inspection.setDriverSig(driverSig);
        inspection.setSupervisorSig(supervisorSig);
        inspection.setSecuritySig(securitySig);
        if (endOdometer != null) inspection.setEndOdometer(endOdometer);
        if (notes != null) inspection.setNotes(notes);

        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Security Check: If it's a DRIVER, they must be the one who started it
        boolean isDriver = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));
        
        if (isDriver && !currentUserId.equals(inspection.getStartedByUserId())) {
            throw new IllegalStateException("Drivers can only end inspections they started.");
        }

        inspection.setSignedByUserId(currentUserId);

        // Automate Shipment Status
        Shipment shipment = inspection.getShipment();
        if (shipment != null) {
            String nextStatus = "PASS".equals(status) || "CONDITIONAL".equals(status) ? "COMPLETED" : "REJECTED";
            statusRepository.findByName(nextStatus).ifPresent(shipment::setStatus);
            shipmentRepository.save(shipment);
        }

        // Update Vehicle last odometer
        if (inspection.getVehicle() != null && inspection.getEndOdometer() != null) {
            Vehicle v = inspection.getVehicle();
            v.setLastOdometer(inspection.getEndOdometer());
            vehicleRepository.save(v);
        }
        
        return toInspectionDTO(inspectionRepository.save(inspection));
    }

    public java.util.Map<String, Long> getZoneAnalytics(UUID id) {
        List<ChecklistItem> items = checklistItemRepository.findByInspection_InspectionIdOrderById(id);
        return items.stream()
                .filter(i -> i.getStartTime() != null && i.getEndTime() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getTemplate().getZoneName(),
                        Collectors.summingLong(i -> java.time.Duration.between(i.getStartTime(), i.getEndTime()).toMillis())
                ));
    }

    public InspectionDTO toInspectionDTO(Inspection inspection) {
        InspectionDTO dto = new InspectionDTO();
        dto.setId(inspection.getInspectionId());
        dto.setGpsLocation(inspection.getGpsLocation());
        dto.setOverallStatus(inspection.getOverallStatus());
        dto.setDriverSig(inspection.getDriverSig());
        dto.setSupervisorSig(inspection.getSupervisorSig());
        dto.setSecuritySig(inspection.getSecuritySig());
        dto.setStartedByUserId(inspection.getStartedByUserId());
        dto.setSignedByUserId(inspection.getSignedByUserId());
        dto.setStartTime(inspection.getStartTime());
        dto.setEndTime(inspection.getEndTime());
        if (inspection.getStartTime() != null && inspection.getEndTime() != null) {
            dto.setTotalDurationMs(Duration.between(inspection.getStartTime(), inspection.getEndTime()).toMillis());
        }
        dto.setStartOdometer(inspection.getStartOdometer());
        dto.setEndOdometer(inspection.getEndOdometer());
        dto.setNotes(inspection.getNotes());
        dto.setCreatedAt(inspection.getCreatedAt());
        dto.setUpdatedAt(inspection.getUpdatedAt());
        if (inspection.getFleetGroup() != null) {
            dto.setFleetGroupId(inspection.getFleetGroup().getId());
            dto.setFleetGroupName(inspection.getFleetGroup().getName());
        }

        if (inspection.getShipment() != null) {
            dto.setShipmentId(inspection.getShipment().getShipmentId());
        }

        if (inspection.getVehicle() != null) {
            dto.setVehicleId(inspection.getVehicle().getVehicleId());
        }
        
        if (inspection.getTrailer() != null) {
            dto.setTrailerId(inspection.getTrailer().getVehicleId());
            dto.setTrailerName(inspection.getTrailer().getVehicleId());
        }

        if (inspection.getDriver() != null) {
            dto.setDriverId(inspection.getDriver().getUserId());
            dto.setDriverName(inspection.getDriver().getFullName());
        }

        if (inspection.getCoDriver() != null) {
            dto.setCoDriverId(inspection.getCoDriver().getUserId());
            dto.setCoDriverName(inspection.getCoDriver().getFullName());
        }

        dto.setTripType(inspection.getTripType());

        if (inspection.getChecklistItems() != null) {
            dto.setChecklistItems(inspection.getChecklistItems().stream()
                    .sorted(Comparator.comparing(ChecklistItem::getId))
                    .map(this::toChecklistItemDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setChecklistItems(new ArrayList<>());
        }

        return dto;
    }

    private ChecklistItemDTO toChecklistItemDTO(ChecklistItem item) {
        ChecklistItemDTO dto = new ChecklistItemDTO();
        dto.setId(item.getId());
        dto.setItemCode(item.getTemplate().getItemCode());
        dto.setItemName(item.getTemplate().getDisplayName());
        dto.setZoneName(item.getTemplate().getZoneName());
        dto.setStatus(item.getStatus());
        dto.setCritical(item.getTemplate().isCritical());
        dto.setResponse(item.getResponse());
        dto.setRemarks(item.getRemarks());
        dto.setPhotoUrl(item.getPhotoUrl());
        dto.setBeforePhotoUrl(item.getBeforePhotoUrl());
        dto.setAfterPhotoUrl(item.getAfterPhotoUrl());
        dto.setFixed(item.isFixed());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        inspectionDefectRepository.findByChecklistItem_Id(item.getId())
                .ifPresent(defect -> dto.setDefectStatus(defect.getStatus()));
        
        if (item.getStartTime() != null && item.getEndTime() != null) {
            java.time.Duration duration = java.time.Duration.between(item.getStartTime(), item.getEndTime());
            dto.setDurationMs(duration.toMillis());
        }
        
        return dto;
    }

    private void createInspectionItems(Inspection inspection) {
        if (checklistItemRepository.countByInspection_InspectionId(inspection.getInspectionId()) > 0) {
            return;
        }

        List<ChecklistItem> items = checklistTemplateRepository.findAll().stream()
                .filter(ChecklistTemplate::isActive)
                .sorted(Comparator.comparing(ChecklistTemplate::getZoneCode).thenComparing(ChecklistTemplate::getItemCode))
                .map(template -> {
                    ChecklistItem item = new ChecklistItem();
                    item.setInspection(inspection);
                    item.setTemplate(template);
                    item.setStatus("PENDING");
                    return item;
                })
                .collect(Collectors.toList());

        checklistItemRepository.saveAll(items);
    }

    private void openOrRefreshDefect(ChecklistItem item, String remarks, String evidencePhoto) {
        InspectionDefect defect = inspectionDefectRepository.findByChecklistItem_Id(item.getId())
                .orElseGet(InspectionDefect::new);
        defect.setInspection(item.getInspection());
        defect.setChecklistItem(item);
        defect.setStatus("OPEN");
        defect.setReportedByUserId(currentUserId());
        defect.setIssuePhotoUrl(evidencePhoto);
        defect.setIssueRemarks(remarks);
        defect.setReportedAt(LocalDateTime.now());
        defect.setResolvedAt(null);
        defect.setResolvedByUserId(null);
        defect.setResolutionPhotoUrl(null);
        defect.setResolutionRemarks(null);
        inspectionDefectRepository.save(defect);
    }

    private Inspection getInspection(UUID inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + inspectionId));
    }

    private ChecklistItem getChecklistItem(UUID inspectionId, String itemCode) {
        getInspection(inspectionId);
        return checklistItemRepository.findByInspection_InspectionIdAndTemplate_ItemCode(inspectionId, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item " + itemCode + " not found for inspection " + inspectionId));
    }

    private InspectionDefectDTO toDefectDTO(InspectionDefect defect) {
        return new InspectionDefectDTO(
                defect.getId(),
                defect.getInspection().getInspectionId(),
                defect.getChecklistItem().getTemplate().getItemCode(),
                defect.getChecklistItem().getTemplate().getDisplayName(),
                defect.getStatus(),
                defect.getReportedByUserId(),
                defect.getResolvedByUserId(),
                defect.getIssuePhotoUrl(),
                defect.getResolutionPhotoUrl(),
                defect.getIssueRemarks(),
                defect.getResolutionRemarks(),
                defect.getReportedAt(),
                defect.getResolvedAt()
        );
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private void validateTripType(Inspection inspection) {
        String tripType = inspection.getTripType();
        if (tripType == null || tripType.isBlank()) {
            tripType = inspection.getCoDriver() == null ? "SINGLE" : "DUO";
            inspection.setTripType(tripType);
        }

        if ("SINGLE".equalsIgnoreCase(tripType)) {
            if (inspection.getCoDriver() != null) {
                throw new IllegalStateException("SINGLE trip type cannot have a co-driver.");
            }
            return;
        }

        if ("DUO".equalsIgnoreCase(tripType)) {
            if (inspection.getCoDriver() == null) {
                throw new IllegalStateException("DUO trip type requires a co-driver.");
            }
            return;
        }

        throw new IllegalStateException("Invalid Trip Type: " + tripType);
    }
}
