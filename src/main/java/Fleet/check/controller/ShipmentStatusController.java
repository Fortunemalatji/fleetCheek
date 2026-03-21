package Fleet.check.controller;

import Fleet.check.dto.LookupDTO;
import Fleet.check.entity.ShipmentStatus;
import Fleet.check.service.ShipmentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lookups/shipment-statuses")
@RequiredArgsConstructor
public class ShipmentStatusController {
    private final ShipmentStatusService service;

    @GetMapping
    public List<LookupDTO> getAll() {
        return service.getAll();
    }

    @PostMapping
    public ShipmentStatus create(@RequestBody ShipmentStatus status) {
        return service.create(status);
    }

    @PutMapping("/{id}")
    public ShipmentStatus update(@PathVariable Integer id, @RequestBody ShipmentStatus details) {
        return service.update(id, details);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
