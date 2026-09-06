package compensation_engine.repository;

import compensation_engine.model.ApplicationAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository
public interface ApplicationAccessRepository
        extends JpaRepository<ApplicationAccess, Long> {

    List<ApplicationAccess> findByEmployeeId(String employeeId);

    List<ApplicationAccess> findByEmployeeIdAndApplicationIgnoreCase(
            String employeeId, String application);

    void deleteByEmployeeIdAndApplicationIgnoreCase(
            String employeeId, String application);

    void deleteByEmployeeId(String employeeId);

        List<ApplicationAccess> findByExpiresAtBeforeAndStatus(Instant expiresAt, String status);
}
