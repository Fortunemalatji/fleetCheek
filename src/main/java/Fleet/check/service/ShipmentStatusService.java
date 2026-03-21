package Fleet.check.service;

import Fleet.check.dto.LookupDTO;
import Fleet.check.entity.ShipmentStatus;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.ShipmentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentStatusService {
    private final ShipmentStatusRepository repository;

    public List<LookupDTO> getAll() {
        return repository.findAll().stream()
                .map(s -> new LookupDTO(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    public ShipmentStatus create(ShipmentStatus status) {
        return repository.save(status);
    }

    public ShipmentStatus update(Integer id, ShipmentStatus details) {
        ShipmentStatus status = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + id));
        status.setName(details.getName());
        return repository.save(status);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
