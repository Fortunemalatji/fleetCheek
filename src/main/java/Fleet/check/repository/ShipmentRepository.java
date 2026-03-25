package Fleet.check.repository;

import Fleet.check.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    List<Shipment> findByDriver_UserIdOrCoDriver_UserId(String driverUserId, String coDriverUserId);
}
