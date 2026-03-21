package Fleet.check.service;

import Fleet.check.dto.VehicleDTO;
import Fleet.check.entity.Vehicle;
import Fleet.check.entity.VehicleCategory;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.VehicleCategoryRepository;
import Fleet.check.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;

    public List<VehicleDTO> getAll() {
        return vehicleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<VehicleDTO> getById(String id) {
        return vehicleRepository.findById(id).map(this::toDTO);
    }

    public VehicleDTO create(VehicleDTO dto) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(dto.getVehicleId());
        if (dto.getLastOdometer() != null) vehicle.setLastOdometer(dto.getLastOdometer().intValue());
        vehicle.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        if (dto.getCategoryId() != null) {
            vehicle.setCategory(categoryRepository.findById(dto.getCategoryId()).orElse(null));
        }
        
        return toDTO(vehicleRepository.save(vehicle));
    }

    public VehicleDTO update(String id, VehicleDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        
        if (dto.getLastOdometer() != null) vehicle.setLastOdometer(dto.getLastOdometer().intValue());
        if (dto.getIsActive() != null) vehicle.setActive(dto.getIsActive());
        
        if (dto.getCategoryId() != null) {
            vehicle.setCategory(categoryRepository.findById(dto.getCategoryId()).orElse(null));
        }
        
        return toDTO(vehicleRepository.save(vehicle));
    }

    public void delete(String id) {
        vehicleRepository.deleteById(id);
    }

    private VehicleDTO toDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setVehicleId(vehicle.getVehicleId());
        dto.setLastOdometer(vehicle.getLastOdometer() != null ? vehicle.getLastOdometer().doubleValue() : null);
        dto.setIsActive(vehicle.isActive());
        dto.setCreatedAt(vehicle.getCreatedAt());
        dto.setUpdatedAt(vehicle.getUpdatedAt());
        if (vehicle.getCategory() != null) {
            dto.setCategoryId(vehicle.getCategory().getId());
            dto.setCategoryName(vehicle.getCategory().getName());
        }
        return dto;
    }
}
