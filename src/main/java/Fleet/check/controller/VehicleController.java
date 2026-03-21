package Fleet.check.controller;

import Fleet.check.dto.VehicleDTO;
import Fleet.check.entity.Vehicle;
import Fleet.check.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;

    @GetMapping
    public List<VehicleDTO> getAll() {
        return vehicleService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getById(@PathVariable String id) {
        return vehicleService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public VehicleDTO create(@RequestBody VehicleDTO vehicle) {
        return vehicleService.create(vehicle);
    }

    @PutMapping("/{id}")
    public VehicleDTO update(@PathVariable String id, @RequestBody VehicleDTO details) {
        return vehicleService.update(id, details);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
