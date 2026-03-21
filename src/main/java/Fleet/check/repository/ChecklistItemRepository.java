package Fleet.check.repository;

import Fleet.check.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByInspection_InspectionId(UUID inspectionId);
    boolean existsByInspection_InspectionIdAndTemplate_ItemCode(UUID inspectionId, String itemCode);
    long countByInspection_InspectionId(UUID inspectionId);
}
