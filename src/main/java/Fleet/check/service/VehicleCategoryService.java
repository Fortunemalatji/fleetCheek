package Fleet.check.service;

import Fleet.check.dto.LookupDTO;
import Fleet.check.entity.VehicleCategory;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.VehicleCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleCategoryService {
    private final VehicleCategoryRepository repository;

    public List<LookupDTO> getAll() {
        return repository.findAll().stream()
                .map(v -> new LookupDTO(v.getId(), v.getName()))
                .collect(Collectors.toList());
    }

    public VehicleCategory create(VehicleCategory category) {
        return repository.save(category);
    }

    public VehicleCategory update(Integer id, VehicleCategory details) {
        VehicleCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setName(details.getName());
        return repository.save(category);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
