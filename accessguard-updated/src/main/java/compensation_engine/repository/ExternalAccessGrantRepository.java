package compensation_engine.repository;

import compensation_engine.model.ExternalAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface ExternalAccessGrantRepository extends JpaRepository<ExternalAccessGrant, String> {

    Optional<ExternalAccessGrant> findByEmployeeIdAndApplicationIgnoreCase(
            String employeeId, String application);

    List<ExternalAccessGrant> findByEmployeeId(String employeeId);

    void deleteByEmployeeId(String employeeId);

    List<ExternalAccessGrant> findByExpiresAtBeforeAndStatus(Instant expiresAt, String status);
}
