package Fleet.check.repository;

import Fleet.check.entity.InspectionDefect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionDefectRepository extends JpaRepository<InspectionDefect, Long> {
    List<InspectionDefect> findByStatusOrderByReportedAtDesc(String status);
    List<InspectionDefect> findByInspection_InspectionIdOrderByReportedAtAsc(UUID inspectionId);
    Optional<InspectionDefect> findByChecklistItem_Id(Long checklistItemId);
}
