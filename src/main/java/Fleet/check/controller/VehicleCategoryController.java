package Fleet.check.controller;

import Fleet.check.dto.LookupDTO;
import Fleet.check.entity.VehicleCategory;
import Fleet.check.service.VehicleCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lookups/vehicle-categories")
@RequiredArgsConstructor
public class VehicleCategoryController {
    private final VehicleCategoryService service;

    @GetMapping
    public List<LookupDTO> getAll() {
        return service.getAll();
    }

    @PostMapping
    public VehicleCategory create(@RequestBody VehicleCategory category) {
        return service.create(category);
    }

    @PutMapping("/{id}")
    public VehicleCategory update(@PathVariable Integer id, @RequestBody VehicleCategory details) {
        return service.update(id, details);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
