package compensation_engine.agent.reconciliation;

import compensation_engine.model.Account;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.model.Resource;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.EmployeeRepository;
import compensation_engine.repository.ExternalAccessGrantRepository;
import compensation_engine.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReconciliationAgent {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final ApplicationAccessRepository accessRepository;
    private final ResourceRepository resourceRepository;
    private final ExternalAccessGrantRepository externalGrantRepository;

    public ReconciliationAgent(EmployeeRepository employeeRepository,
                               AccountRepository accountRepository,
                               ApplicationAccessRepository accessRepository,
                               ResourceRepository resourceRepository,
                               ExternalAccessGrantRepository externalGrantRepository) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.accessRepository = accessRepository;
        this.resourceRepository = resourceRepository;
        this.externalGrantRepository = externalGrantRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport scan() {
        List<Employee> employees = employeeRepository.findAll();
        List<Account> accounts = accountRepository.findAll();
        List<ApplicationAccess> applicationAccess = accessRepository.findAll();
        List<Resource> resources = resourceRepository.findAll();
        List<ExternalAccessGrant> externalGrants = externalGrantRepository.findAll();
        List<DriftFinding> findings = new ArrayList<>();

        Set<String> accountEmployees = activeEmployeeIds(accounts, Account::getEmployeeId);
        Set<String> accessEmployees = activeEmployeeIds(applicationAccess, ApplicationAccess::getEmployeeId);
        Set<String> resourceEmployees = activeEmployeeIds(resources, Resource::getEmployeeId);
        Set<String> externalAccessEmployees = activeEmployeeIds(externalGrants, ExternalAccessGrant::getEmployeeId);

        for (Employee employee : employees) {
            String employeeId = employee.getEmployeeId();
            boolean active = "ACTIVE".equalsIgnoreCase(employee.getStatus());

            if (!active) {
                addIfPresent(findings, accountEmployees.contains(employeeId),
                        "INACTIVE_ACCOUNT_ACTIVE", DriftSeverity.CRITICAL, employeeId, "Account",
                        "Inactive employee has no active account record expected, but lifecycle state must be verified.",
                        "Verify account state and disable it if still active.");
                addIfPresent(findings, accessEmployees.contains(employeeId),
                        "INACTIVE_APPLICATION_ACCESS", DriftSeverity.CRITICAL, employeeId, "ApplicationAccess",
                        "Inactive employee still has active application access.",
                        "Revoke all application access through an approved offboarding workflow.");
                addIfPresent(findings, externalAccessEmployees.contains(employeeId),
                        "INACTIVE_EXTERNAL_ACCESS", DriftSeverity.CRITICAL, employeeId, "ExternalAccess",
                        "Inactive employee still has active access in an external connector.",
                        "Revoke external access and investigate connector state.");
                addIfPresent(findings, resourceEmployees.contains(employeeId),
                        "INACTIVE_RESOURCE", DriftSeverity.HIGH, employeeId, "Resource",
                        "Inactive employee still owns active resources.",
                        "Review and revoke resources through an approved remediation workflow.");
            } else {
                addIfPresent(findings, !accountEmployees.contains(employeeId),
                        "ACTIVE_EMPLOYEE_MISSING_ACCOUNT", DriftSeverity.HIGH, employeeId, "Account",
                        "Active employee has no active account.",
                        "Run or review onboarding account provisioning.");
                addIfPresent(findings, !resourceEmployees.contains(employeeId),
                        "ACTIVE_EMPLOYEE_MISSING_RESOURCE", DriftSeverity.MEDIUM, employeeId, "Resource",
                        "Active employee has no active managed resource.",
                        "Review whether required resources were provisioned.");
            }
        }

        ReconciliationReport report = new ReconciliationReport();
        report.setScannedAt(Instant.now());
        report.setEmployeesScanned(employees.size());
        report.setFindings(findings);
        return report;
    }

    private <T> Set<String> activeEmployeeIds(List<T> records, Function<T, String> employeeId) {
        return records.stream()
                .filter(record -> isActive(statusOf(record)))
                .map(employeeId)
                .collect(Collectors.toSet());
    }

    private String statusOf(Object record) {
        if (record instanceof Account account) return account.getStatus();
        if (record instanceof ApplicationAccess access) return access.getStatus();
        if (record instanceof Resource resource) return resource.getStatus();
        if (record instanceof ExternalAccessGrant grant) return grant.getStatus();
        return null;
    }

    private boolean isActive(String status) {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    private void addIfPresent(List<DriftFinding> findings, boolean condition,
                              String code, DriftSeverity severity, String employeeId,
                              String resourceType, String message, String action) {
        if (condition) {
            findings.add(new DriftFinding(code, severity, employeeId, resourceType, message, action));
        }
    }
}
