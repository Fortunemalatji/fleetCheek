package Fleet.check.repository;

import Fleet.check.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {
    Optional<ChecklistTemplate> findByItemCode(String itemCode);
    List<ChecklistTemplate> findByZoneCode(String zoneCode);
}
