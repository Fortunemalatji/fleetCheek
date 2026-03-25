package Fleet.check.service;

import lombok.extern.slf4j.Slf4j;

import Fleet.check.dto.VehicleDTO;
import Fleet.check.entity.Vehicle;
import Fleet.check.entity.FleetGroup;
import Fleet.check.entity.VehicleCategory;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.FleetGroupRepository;
import Fleet.check.repository.VehicleCategoryRepository;
import Fleet.check.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final FleetGroupRepository fleetGroupRepository;

    public List<VehicleDTO> getAll() {
        return vehicleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<VehicleDTO> getById(String id) {
        return vehicleRepository.findById(id).map(this::toDTO);
    }

    public VehicleDTO create(VehicleDTO dto) {
        log.info("Creating vehicle with DTO: {}", dto);
        if (dto.getCategoryId() == null) {
            throw new IllegalStateException("Vehicle categoryId is required.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(dto.getVehicleId());
        vehicle.setRegistration(dto.getRegistration());
        vehicle.setName(dto.getName());
        vehicle.setMake(dto.getMake());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        if (dto.getLastOdometer() != null) vehicle.setLastOdometer(dto.getLastOdometer().intValue());
        vehicle.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        vehicle.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle category not found: " + dto.getCategoryId())));

        vehicle.setPhotoUrl(dto.getPhotoUrl());
        if (dto.getFleetGroupId() != null) {
            vehicle.setFleetGroup(fleetGroupRepository.findById(dto.getFleetGroupId()).orElse(null));
        } else if (dto.getFleetGroupName() != null) {
            vehicle.setFleetGroup(fleetGroupRepository.findByName(dto.getFleetGroupName())
                .orElseGet(() -> fleetGroupRepository.save(new FleetGroup(null, dto.getFleetGroupName()))));
        }
        
        return toDTO(vehicleRepository.save(vehicle));
    }

    public VehicleDTO update(String id, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        
        if (dto.getRegistration() != null) vehicle.setRegistration(dto.getRegistration());
        if (dto.getName() != null) vehicle.setName(dto.getName());
        if (dto.getMake() != null) vehicle.setMake(dto.getMake());
        if (dto.getModel() != null) vehicle.setModel(dto.getModel());
        if (dto.getYear() != null) vehicle.setYear(dto.getYear());
        if (dto.getLastOdometer() != null) vehicle.setLastOdometer(dto.getLastOdometer().intValue());
        if (dto.getIsActive() != null) vehicle.setActive(dto.getIsActive());
        
        if (dto.getCategoryId() != null) {
            vehicle.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle category not found: " + dto.getCategoryId())));
        }

        if (dto.getPhotoUrl() != null) vehicle.setPhotoUrl(dto.getPhotoUrl());
        if (dto.getFleetGroupId() != null) {
            vehicle.setFleetGroup(fleetGroupRepository.findById(dto.getFleetGroupId()).orElse(null));
        } else if (dto.getFleetGroupName() != null) {
            vehicle.setFleetGroup(fleetGroupRepository.findByName(dto.getFleetGroupName())
                .orElseGet(() -> fleetGroupRepository.save(new FleetGroup(null, dto.getFleetGroupName()))));
        }
        
        return toDTO(vehicleRepository.save(vehicle));
    }

    public void delete(String id) {
        vehicleRepository.deleteById(id);
    }

    private VehicleDTO toDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setVehicleId(vehicle.getVehicleId());
        dto.setRegistration(vehicle.getRegistration());
        dto.setName(vehicle.getName());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setLastOdometer(vehicle.getLastOdometer() != null ? vehicle.getLastOdometer().doubleValue() : null);
        dto.setIsActive(vehicle.isActive());
        dto.setCreatedAt(vehicle.getCreatedAt());
        dto.setUpdatedAt(vehicle.getUpdatedAt());
        if (vehicle.getCategory() != null) {
            dto.setCategoryId(vehicle.getCategory().getId());
            dto.setCategoryName(vehicle.getCategory().getName());
        }
        dto.setPhotoUrl(vehicle.getPhotoUrl());
        if (vehicle.getFleetGroup() != null) {
            dto.setFleetGroupId(vehicle.getFleetGroup().getId());
            dto.setFleetGroupName(vehicle.getFleetGroup().getName());
        }
        return dto;
    }
}
