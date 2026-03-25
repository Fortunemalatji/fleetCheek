package Fleet.check.repository;

import Fleet.check.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentStatusRepository extends JpaRepository<ShipmentStatus, Integer> {
    java.util.Optional<ShipmentStatus> findByName(String name);
}
