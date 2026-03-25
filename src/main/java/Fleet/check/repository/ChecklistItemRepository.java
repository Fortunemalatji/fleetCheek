package Fleet.check.repository;

import Fleet.check.entity.ChecklistItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByInspection_InspectionId(UUID inspectionId);
    List<ChecklistItem> findByInspection_InspectionIdOrderById(UUID inspectionId);
    boolean existsByInspection_InspectionIdAndTemplate_ItemCode(UUID inspectionId, String itemCode);
    Optional<ChecklistItem> findByInspection_InspectionIdAndTemplate_ItemCode(UUID inspectionId, String itemCode);
    long countByInspection_InspectionId(UUID inspectionId);
    long countByInspection_InspectionIdAndStatus(UUID inspectionId, String status);
    boolean existsByInspection_InspectionIdAndStatus(UUID inspectionId, String status);

    @EntityGraph(attributePaths = {"inspection", "template"})
    List<ChecklistItem> findAllBy();
}
