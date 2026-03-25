package Fleet.check.repository;

import Fleet.check.entity.FleetGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FleetGroupRepository extends JpaRepository<FleetGroup, Integer> {
    Optional<FleetGroup> findByName(String name);
}
