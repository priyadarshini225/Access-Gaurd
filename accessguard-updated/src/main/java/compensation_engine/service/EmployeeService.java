package compensation_engine.service;

import compensation_engine.exception.AmbiguousEmployeeNameException;
import compensation_engine.exception.EmployeeAlreadyExistsException;
import compensation_engine.exception.EmployeeNotFoundException;
import compensation_engine.model.Employee;
import compensation_engine.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final compensation_engine.agent.identity.IdentityResolutionAgent identityResolutionAgent;

    public EmployeeService(EmployeeRepository employeeRepository,
                           compensation_engine.agent.identity.IdentityResolutionAgent identityResolutionAgent) {
        this.employeeRepository = employeeRepository;
        this.identityResolutionAgent = identityResolutionAgent;
    }

    // ----------------------------------------------------------------
    // ID GENERATION
    // ----------------------------------------------------------------

    /**
     * Generates the next sequential numeric Employee ID (e.g. 101, 102, 103...).
     * Starts at 101 if the database is empty.
     */
    @Transactional(readOnly = true)
    public synchronized String generateNextEmployeeId() {
        List<Employee> all = employeeRepository.findAll();
        long maxId = 100;
        for (Employee emp : all) {
            String idStr = emp.getEmployeeId();
            if (idStr != null) {
                try {
                    long val = Long.parseLong(idStr.trim());
                    if (val > maxId) {
                        maxId = val;
                    }
                } catch (NumberFormatException ignored) {
                    // If previously formatted like EMP-101
                    if (idStr.startsWith("EMP-")) {
                        try {
                            long val = Long.parseLong(idStr.substring(4).trim());
                            if (val > maxId) maxId = val;
                        } catch (NumberFormatException e) {
                            // ignore non-numeric suffixes
                        }
                    }
                }
            }
        }
        long candidate = maxId + 1;
        while (employeeRepository.existsById(String.valueOf(candidate))) {
            candidate++;
        }
        return String.valueOf(candidate);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String employeeId) {
        return employeeId != null && employeeRepository.existsById(employeeId);
    }

    // ----------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------

    @Transactional
    public Employee createEmployee(String id, String name,
                                   String department, String role) {
        validate(id, "Employee ID");
        validate(name, "Employee name");
        validate(department, "Department");
        validate(role, "Role");

        if (employeeRepository.existsById(id)) {
            throw new EmployeeAlreadyExistsException(id);
        }

        Employee employee = new Employee(id, name, department, role, "ACTIVE");
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee: id={}, name={}", id, name);
        return saved;
    }

    // ----------------------------------------------------------------
    // LOOKUP
    // ----------------------------------------------------------------

    /**
     * Find employee by exact ID.
     */
    @Transactional(readOnly = true)
    public Employee findById(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
    }

    /**
     * Resolve an employee ID safely for offboarding.
     *
     * Rules:
     * - If employeeId is provided: look it up directly.
     * - If only name is provided: search by name.
     *   - Zero results  → EmployeeNotFoundException
     *   - One result    → return that employee's ID
     *   - Many results  → AmbiguousEmployeeNameException (409)
     *
     * This replaces the broken makeId(name) heuristic.
     */
    /**
     * Resolve an employee ID safely for offboarding.
     *
     * Rules:
     * 1. Try direct ID lookup if employeeId is provided.
     * 2. If employeeId is not found as an ID, check if it was entered as a name.
     * 3. If name is provided, search by name.
     * 4. If name is not found as a name, check if it was entered as an ID.
     * 5. If multiple matches: throw AmbiguousEmployeeNameException (409).
     * 6. If no match: throw EmployeeNotFoundException (404).
     */
    @Transactional(readOnly = true)
    public String resolveEmployeeId(String employeeId, String name) {
        compensation_engine.agent.identity.IdentityResolutionResult result =
                identityResolutionAgent.resolve(employeeId, name, null);

        if (result.getStatus() == compensation_engine.agent.identity.IdentityResolutionResult.Status.EXACT_MATCH) {
            return result.getResolvedEmployeeId();
        } else if (result.getStatus() == compensation_engine.agent.identity.IdentityResolutionResult.Status.AMBIGUOUS_MATCH) {
            List<String> candidateIds = result.getCandidates().stream()
                    .map(compensation_engine.agent.identity.CandidateMatch::getEmployeeId)
                    .collect(Collectors.toList());
            String query = (name != null && !name.isBlank()) ? name : employeeId;
            throw new AmbiguousEmployeeNameException(query, candidateIds);
        } else {
            String target = (name != null && !name.isBlank()) ? name : (employeeId != null ? employeeId : "");
            if (target.isBlank()) {
                throw new IllegalArgumentException("Either employeeId or employee name must be provided.");
            }
            throw new EmployeeNotFoundException(target);
        }
    }

    // ----------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------

    @Transactional
    public void deactivateEmployee(String employeeId) {
        validate(employeeId, "Employee ID");

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        if ("INACTIVE".equalsIgnoreCase(employee.getStatus())) {
            log.info("Employee {} is already INACTIVE — skipping deactivation.", employeeId);
            return;
        }

        employee.setStatus("INACTIVE");
        employeeRepository.save(employee);
        log.info("Deactivated employee: {}", employeeId);
    }

    // ----------------------------------------------------------------
    // DELETE (compensation only)
    // ----------------------------------------------------------------

    @Transactional
    public void deleteEmployee(String employeeId) {
        employeeRepository.deleteById(employeeId);
        log.info("Deleted employee (compensation): {}", employeeId);
    }

    // ----------------------------------------------------------------
    // QUERY
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    // ----------------------------------------------------------------
    // VALIDATION
    // ----------------------------------------------------------------

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
