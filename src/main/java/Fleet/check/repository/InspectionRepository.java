package Fleet.check.repository;

import Fleet.check.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, UUID> {
    List<Inspection> findDistinctByShipment_Driver_UserIdOrShipment_CoDriver_UserIdOrDriver_UserIdOrCoDriver_UserIdOrStartedByUserIdOrSignedByUserId(
            String shipmentDriverUserId,
            String shipmentCoDriverUserId,
            String driverUserId,
            String coDriverUserId,
            String startedByUserId,
            String signedByUserId
    );
}
