package compensation_engine.service;

import compensation_engine.model.ApplicationAccess;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccessService {

    private static final Logger log =
            LoggerFactory.getLogger(AccessService.class);

    private final ApplicationAccessRepository accessRepository;
    private final EmployeeRepository employeeRepository;

    public AccessService(ApplicationAccessRepository accessRepository,
                         EmployeeRepository employeeRepository) {
        this.accessRepository = accessRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public ApplicationAccess grantAccess(String employeeId,
                                         String application,
                                         String accessLevel) {
        validate(employeeId, "Employee ID");
        validate(application, "Application");
        validate(accessLevel, "Access level");

        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalStateException(
                    "Cannot grant access. Employee not found: " + employeeId);
        }

        // Remove existing access for same app (upsert pattern)
        accessRepository.deleteByEmployeeIdAndApplicationIgnoreCase(
                employeeId, application);

        ApplicationAccess access = new ApplicationAccess(
                employeeId, application, accessLevel, "ACTIVE");
        ApplicationAccess saved = accessRepository.save(access);
        log.info("Granted {} access to {} for employee {}",
                accessLevel, application, employeeId);
        return saved;
    }

    /**
     * OFFBOARDING PATH — strict.
     * Throws DataInconsistencyException if access record does not exist.
     */
    @Transactional
    public void removeAccess(String employeeId, String application) {
        validate(employeeId, "Employee ID");
        validate(application, "Application");

        List<ApplicationAccess> existing =
                accessRepository.findByEmployeeIdAndApplicationIgnoreCase(
                        employeeId, application);

        if (existing.isEmpty()) {
            throw new compensation_engine.exception.DataInconsistencyException(
                    "ApplicationAccess(" + application + ")", employeeId);
        }

        accessRepository.deleteByEmployeeIdAndApplicationIgnoreCase(employeeId, application);
        log.info("Removed {} access for employee {}", application, employeeId);
    }

    /**
     * ADMIN / SABOTAGE PATH.
     * Hard-deletes all application access records so removeAccess() will fail during offboarding.
     */
    @Transactional
    public void sabotageAccess(String employeeId) {
        validate(employeeId, "Employee ID");
        accessRepository.deleteByEmployeeId(employeeId);
        log.warn("ADMIN SABOTAGE: All ApplicationAccess records for employee {} force-deleted from database.", employeeId);
    }

    /** Remove ALL application access for an employee during full offboarding. */
    @Transactional
    public void removeAllAccess(String employeeId) {
        validate(employeeId, "Employee ID");
        accessRepository.deleteByEmployeeId(employeeId);
        log.info("Removed all application access for employee {}", employeeId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationAccess> findAll() {
        return accessRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApplicationAccess> findByEmployee(String employeeId) {
        return accessRepository.findByEmployeeId(employeeId);
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
