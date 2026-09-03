package compensation_engine.service;

import compensation_engine.model.Resource;
import compensation_engine.repository.EmployeeRepository;
import compensation_engine.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceService {

    private static final Logger log =
            LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository resourceRepository;
    private final EmployeeRepository employeeRepository;

    public ResourceService(ResourceRepository resourceRepository,
                           EmployeeRepository employeeRepository) {
        this.resourceRepository = resourceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Resource createResource(String employeeId, String type) {
        validate(employeeId, "Employee ID");
        validate(type, "Resource type");

        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalStateException(
                    "Cannot create resource. Employee not found: " + employeeId);
        }

        String resourceId = "RES-" + employeeId + "-" +
                type.replaceAll("\\s+", "-");

        Resource resource = new Resource(resourceId, employeeId, type, "ACTIVE");
        Resource saved = resourceRepository.save(resource);
        log.info("Created resource: {} for employee {}", resourceId, employeeId);
        return saved;
    }

    /**
     * OFFBOARDING PATH — strict.
     * Throws DataInconsistencyException if no resource records exist.
     */
    @Transactional
    public void deleteResources(String employeeId) {
        validate(employeeId, "Employee ID");

        List<Resource> resources = resourceRepository.findByEmployeeId(employeeId);

        if (resources.isEmpty()) {
            throw new compensation_engine.exception.DataInconsistencyException("Resource", employeeId);
        }

        resourceRepository.deleteByEmployeeId(employeeId);
        log.info("Deleted {} resource(s) for employee {}", resources.size(), employeeId);
    }

    /**
     * ADMIN / SABOTAGE PATH.
     * Hard-deletes all resource records so deleteResources() will fail during offboarding.
     */
    @Transactional
    public void sabotageResources(String employeeId) {
        validate(employeeId, "Employee ID");
        resourceRepository.deleteByEmployeeId(employeeId);
        log.warn("ADMIN SABOTAGE: Resource records for employee {} force-deleted from database.", employeeId);
    }

    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
