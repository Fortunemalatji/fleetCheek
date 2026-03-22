package Fleet.check.service;

import Fleet.check.dto.ShipmentDTO;
import Fleet.check.entity.Shipment;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentStatusRepository statusRepository;

    public List<ShipmentDTO> getAll() {
        return shipmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ShipmentDTO> getById(String id) {
        return shipmentRepository.findById(id).map(this::toDTO);
    }

    public ShipmentDTO create(ShipmentDTO dto) {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(dto.getShipmentId());
        shipment.setDispatchDate(dto.getDispatchDate());
        
        if (dto.getDriverId() != null) {
            shipment.setDriver(userRepository.findById(dto.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found")));
        }
        
        if (dto.getVehicleId() != null) {
            shipment.setVehicle(vehicleRepository.findById(dto.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found")));
        }

        if (dto.getTrailerId() != null) {
            shipment.setTrailer(vehicleRepository.findById(dto.getTrailerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trailer not found")));
        }
        
        if (dto.getStatusId() != null) {
            shipment.setStatus(statusRepository.findById(dto.getStatusId()).orElse(null));
        } else {
            // Default to first status if not provided (usually DISPATCHED/PENDING)
            shipment.setStatus(statusRepository.findAll().stream().findFirst().orElse(null));
        }

        return toDTO(shipmentRepository.save(shipment));
    }

    public ShipmentDTO update(String id, ShipmentDTO dto) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));
        
        if (dto.getDispatchDate() != null) shipment.setDispatchDate(dto.getDispatchDate());
        
        if (dto.getDriverId() != null) {
            shipment.setDriver(userRepository.findById(dto.getDriverId()).orElse(null));
        }
        
        if (dto.getVehicleId() != null) {
            shipment.setVehicle(vehicleRepository.findById(dto.getVehicleId()).orElse(null));
        }

        if (dto.getTrailerId() != null) {
            shipment.setTrailer(vehicleRepository.findById(dto.getTrailerId()).orElse(null));
        }
        
        if (dto.getStatusId() != null) {
            shipment.setStatus(statusRepository.findById(dto.getStatusId()).orElse(null));
        }
        
        return toDTO(shipmentRepository.save(shipment));
    }

    public void delete(String id) {
        shipmentRepository.deleteById(id);
    }

    private ShipmentDTO toDTO(Shipment shipment) {
        ShipmentDTO dto = new ShipmentDTO();
        dto.setShipmentId(shipment.getShipmentId());
        dto.setDispatchDate(shipment.getDispatchDate());
        dto.setCreatedAt(shipment.getCreatedAt());
        dto.setUpdatedAt(shipment.getUpdatedAt());
        
        if (shipment.getDriver() != null) {
            dto.setDriverId(shipment.getDriver().getUserId());
            dto.setDriverName(shipment.getDriver().getFullName());
        }
        
        if (shipment.getVehicle() != null) {
            dto.setVehicleId(shipment.getVehicle().getVehicleId());
        }

        if (shipment.getTrailer() != null) {
            dto.setTrailerId(shipment.getTrailer().getVehicleId());
            dto.setTrailerName(shipment.getTrailer().getVehicleId()); // Or some other name if available
        }
        
        if (shipment.getStatus() != null) {
            dto.setStatusId(shipment.getStatus().getId());
            dto.setStatusName(shipment.getStatus().getName());
        }
        
        return dto;
    }
}
