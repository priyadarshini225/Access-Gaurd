package compensation_engine.repository;

import compensation_engine.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    List<AuditEvent> findTop100ByOrderByOccurredAtDesc();

    List<AuditEvent> findByRequestIdOrderByOccurredAtAsc(String requestId);
}
