package compensation_engine.controller;

import compensation_engine.repository.*;
import compensation_engine.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only controller for deliberate data sabotage and database management.
 *
 * These endpoints physically delete specific database records for an employee
 * WITHOUT going through the normal workflow.  This creates a real data
 * inconsistency: when you then run offboarding, the workflow genuinely fails
 * at the step whose record is missing — not a simulation, a real DB miss.
 *
 * Also provides complete database wipe functionality for fresh demo resets.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminDataController {

    private static final Logger log = LoggerFactory.getLogger(AdminDataController.class);

    private final EmailService emailService;
    private final AccountService accountService;
    private final ResourceService resourceService;
    private final AccessService accessService;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final EmailRepository emailRepository;
    private final ApplicationAccessRepository accessRepository;
    private final ResourceRepository resourceRepository;
    private final WorkflowExecutionRepository executionRepository;

    public AdminDataController(EmailService emailService,
                               AccountService accountService,
                               ResourceService resourceService,
                               AccessService accessService,
                               EmployeeRepository employeeRepository,
                               AccountRepository accountRepository,
                               EmailRepository emailRepository,
                               ApplicationAccessRepository accessRepository,
                               ResourceRepository resourceRepository,
                               WorkflowExecutionRepository executionRepository) {
        this.emailService        = emailService;
        this.accountService      = accountService;
        this.resourceService     = resourceService;
        this.accessService       = accessService;
        this.employeeRepository  = employeeRepository;
        this.accountRepository   = accountRepository;
        this.emailRepository     = emailRepository;
        this.accessRepository    = accessRepository;
        this.resourceRepository  = resourceRepository;
        this.executionRepository = executionRepository;
    }

    /**
     * POST /api/admin/reset-database
     * Completely wipes all records across all tables in the persistent store.
     */
    @PostMapping("/reset-database")
    @Transactional
    public ResponseEntity<Map<String, String>> resetDatabase() {
        log.warn("ADMIN: Purging all database records across all tables.");
        accessRepository.deleteAll();
        emailRepository.deleteAll();
        resourceRepository.deleteAll();
        accountRepository.deleteAll();
        employeeRepository.deleteAll();
        executionRepository.deleteAll();

        return ResponseEntity.ok(Map.of(
                "status", "RESET_SUCCESS",
                "message", "All database records have been purged. The database is now completely empty."
        ));
    }

    /**
     * DELETE /api/admin/sabotage/email/{employeeId}
     */
    @DeleteMapping("/sabotage/email/{employeeId}")
    public ResponseEntity<Map<String, String>> sabotageEmail(@PathVariable String employeeId) {
        log.warn("Admin sabotage request: DELETE email record for employee {}", employeeId);
        emailService.sabotageEmail(employeeId);
        return ok("email", employeeId,
                "Email record deleted. Run offboarding to see the 'Remove Email' step fail with a real data inconsistency.");
    }

    /**
     * DELETE /api/admin/sabotage/account/{employeeId}
     */
    @DeleteMapping("/sabotage/account/{employeeId}")
    public ResponseEntity<Map<String, String>> sabotageAccount(@PathVariable String employeeId) {
        log.warn("Admin sabotage request: DELETE account records for employee {}", employeeId);
        accountService.sabotageAccount(employeeId);
        return ok("account", employeeId,
                "Account record deleted. Run offboarding to see the 'Disable Account' step fail.");
    }

    /**
     * DELETE /api/admin/sabotage/resource/{employeeId}
     */
    @DeleteMapping("/sabotage/resource/{employeeId}")
    public ResponseEntity<Map<String, String>> sabotageResource(@PathVariable String employeeId) {
        log.warn("Admin sabotage request: DELETE resource records for employee {}", employeeId);
        resourceService.sabotageResources(employeeId);
        return ok("resource", employeeId,
                "Resource record deleted. Run offboarding to see the 'Revoke Resources' step fail.");
    }

    /**
     * DELETE /api/admin/sabotage/access/{employeeId}
     */
    @DeleteMapping("/sabotage/access/{employeeId}")
    public ResponseEntity<Map<String, String>> sabotageAccess(@PathVariable String employeeId) {
        log.warn("Admin sabotage request: DELETE access records for employee {}", employeeId);
        accessService.sabotageAccess(employeeId);
        return ok("access", employeeId,
                "Access record deleted. Run offboarding to see the 'Remove Application Access' step fail.");
    }

    private ResponseEntity<Map<String, String>> ok(String recordType,
                                                    String employeeId,
                                                    String advice) {
        return ResponseEntity.ok(Map.of(
                "status",     "SABOTAGED",
                "recordType", recordType,
                "employeeId", employeeId,
                "advice",     advice
        ));
    }
}
