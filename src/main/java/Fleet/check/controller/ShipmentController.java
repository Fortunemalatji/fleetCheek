package Fleet.check.controller;

import Fleet.check.dto.ShipmentDTO;
import Fleet.check.entity.Shipment;
import Fleet.check.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    @GetMapping
    public List<ShipmentDTO> getAll() {
        return shipmentService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDTO> getById(@PathVariable String id) {
        return shipmentService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<ShipmentDTO> getByUserId(@PathVariable String userId) {
        return shipmentService.getByUserId(userId);
    }

    @PostMapping
    public ShipmentDTO create(@RequestBody ShipmentDTO shipment) {
        return shipmentService.create(shipment);
    }

    @PutMapping("/{id}")
    public ShipmentDTO update(@PathVariable String id, @RequestBody ShipmentDTO details) {
        return shipmentService.update(id, details);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
