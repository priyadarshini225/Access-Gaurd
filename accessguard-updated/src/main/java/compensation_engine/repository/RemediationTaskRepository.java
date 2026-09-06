package compensation_engine.repository;

import compensation_engine.model.RemediationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemediationTaskRepository extends JpaRepository<RemediationTaskEntity, String> {

    List<RemediationTaskEntity> findTop100ByOrderByCreatedAtDesc();
}
