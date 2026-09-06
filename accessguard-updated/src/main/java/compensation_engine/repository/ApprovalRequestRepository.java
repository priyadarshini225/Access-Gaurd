package compensation_engine.repository;

import compensation_engine.model.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {

    List<ApprovalRequest> findTop20ByOrderByCreatedAtDesc();

    java.util.Optional<ApprovalRequest> findByIdempotencyKey(String idempotencyKey);
}
