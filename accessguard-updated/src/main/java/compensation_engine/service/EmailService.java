package compensation_engine.service;

import compensation_engine.model.Email;
import compensation_engine.model.Employee;
import compensation_engine.repository.EmailRepository;
import compensation_engine.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final EmailRepository emailRepository;
    private final EmployeeRepository employeeRepository;

    public EmailService(EmailRepository emailRepository,
                        EmployeeRepository employeeRepository) {
        this.emailRepository = emailRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Email createEmail(String employeeId) {
        validate(employeeId, "Employee ID");

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot create email. Employee not found: " + employeeId));

        String cleanName = employee.getName()
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", ".");

        cleanName = cleanName.replaceAll("[^a-z0-9]", "");
        if (cleanName.isBlank()) {
            cleanName = "user";
        }

        // Unique combination of name and ID: e.g. kavya.sharma.101@company.com
        String address = cleanName + "@company.local";
        Email email = new Email(employeeId, address);
        Email saved = emailRepository.save(email);
        log.info("Created email: {} for employee {}", address, employeeId);
        return saved;
    }

    /**
     * OFFBOARDING PATH — strict.
     *
     * Throws DataInconsistencyException if the email record is missing.
     * This is intentional: if the email does not exist when offboarding runs,
     * it means a real data inconsistency occurred that an administrator must
     * investigate and resolve manually.
     */
    @Transactional
    public void deleteEmail(String employeeId) {
        validate(employeeId, "Employee ID");

        Optional<Email> email = emailRepository.findById(employeeId);

        if (email.isEmpty()) {
            throw new compensation_engine.exception.DataInconsistencyException("Email", employeeId);
        }

        emailRepository.deleteById(employeeId);
        log.info("Deleted email for employee {}", employeeId);
    }

    /**
     * ADMIN / SABOTAGE PATH — used by AdminDataController.
     *
     * Hard-deletes the email record without throwing if it is already missing.
     * Call this to simulate a real-world data inconsistency before running
     * offboarding, so that the offboarding workflow genuinely fails at the
     * "Remove Email" step.
     */
    @Transactional
    public void sabotageEmail(String employeeId) {
        validate(employeeId, "Employee ID");
        emailRepository.deleteById(employeeId);
        log.warn("ADMIN SABOTAGE: Email record for employee {} force-deleted from database.", employeeId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> findAllAsMap() {
        return emailRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Email::getEmployeeId,
                        Email::getEmailAddress
                ));
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
