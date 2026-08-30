package compensation_engine.tools;

import compensation_engine.model.Account;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.model.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccessManagementTools {

    private final Map<String, Employee> employees = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<ApplicationAccess> applicationAccess = new ArrayList<>();
    private final Map<String, Resource> resources = new LinkedHashMap<>();
    private final Map<String, String> emails = new LinkedHashMap<>();

    // ============================================================
    // ONBOARDING
    // ============================================================

    public synchronized Employee createEmployee(
            String id,
            String name,
            String department,
            String role) {

        validate(id, "Employee ID");
        validate(name, "Employee name");
        validate(department, "Department");
        validate(role, "Role");

        if (employees.containsKey(id)) {
            throw new RuntimeException(
                    "Employee already exists: " + id
            );
        }

        Employee employee = new Employee(
                id,
                name,
                department,
                role,
                "ACTIVE"
        );

        employees.put(id, employee);

        return employee;
    }

    public synchronized void deleteEmployee(String employeeId) {

        employees.remove(employeeId);
    }

    public synchronized Account createAccount(
            String employeeId,
            String username) {

        validate(employeeId, "Employee ID");
        validate(username, "Username");

        Employee employee = employees.get(employeeId);

        if (employee == null) {
            throw new RuntimeException(
                    "Cannot create account. Employee not found: "
                            + employeeId
            );
        }

        String accountId = "ACC-" + employeeId;

        if (accounts.containsKey(accountId)) {
            throw new RuntimeException(
                    "Account already exists for employee: "
                            + employeeId
            );
        }

        Account account = new Account(
                accountId,
                employeeId,
                username,
                "ACTIVE"
        );

        accounts.put(accountId, account);

        return account;
    }

    public synchronized void deleteAccount(
            String employeeId) {

        accounts.values().removeIf(
                account ->
                        employeeId.equals(account.getEmployeeId())
        );
    }

    public synchronized ApplicationAccess grantApplicationAccess(
            String employeeId,
            String application,
            String accessLevel) {

        validate(employeeId, "Employee ID");
        validate(application, "Application");
        validate(accessLevel, "Access level");

        if (!employees.containsKey(employeeId)) {
            throw new RuntimeException(
                    "Cannot grant access. Employee not found: "
                            + employeeId
            );
        }

        /*
         * Do not create duplicate access records.
         */
        applicationAccess.removeIf(
                access ->
                        employeeId.equals(access.getEmployeeId())
                                && application.equalsIgnoreCase(
                                access.getApplication())
        );

        ApplicationAccess access =
                new ApplicationAccess(
                        employeeId,
                        application,
                        accessLevel,
                        "ACTIVE"
                );

        applicationAccess.add(access);

        return access;
    }

    public synchronized void removeApplicationAccess(
            String employeeId,
            String application) {

        validate(employeeId, "Employee ID");
        validate(application, "Application");

        applicationAccess.removeIf(
                access ->
                        employeeId.equals(access.getEmployeeId())
                                && application.equalsIgnoreCase(
                                access.getApplication())
        );
    }

    public synchronized Resource createResource(
            String employeeId,
            String type) {

        validate(employeeId, "Employee ID");
        validate(type, "Resource type");

        if (!employees.containsKey(employeeId)) {
            throw new RuntimeException(
                    "Cannot create resource. Employee not found: "
                            + employeeId
            );
        }

        String resourceId =
                "RES-"
                        + employeeId
                        + "-"
                        + type.replaceAll("\\s+", "-");

        Resource resource =
                new Resource(
                        resourceId,
                        employeeId,
                        type,
                        "ACTIVE"
                );

        resources.put(resourceId, resource);

        return resource;
    }

    public synchronized void deleteResource(
            String employeeId) {

        validate(employeeId, "Employee ID");

        resources.values().removeIf(
                resource ->
                        employeeId.equals(resource.getEmployeeId())
        );
    }

    public synchronized void createEmail(
            String employeeId) {

        validate(employeeId, "Employee ID");

        Employee employee = employees.get(employeeId);

        if (employee == null) {
            throw new RuntimeException(
                    "Cannot create email. Employee not found: "
                            + employeeId
            );
        }

        String cleanName =
                employee.getName()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]", "");

        if (cleanName.isBlank()) {
            throw new RuntimeException(
                    "Cannot create email. Employee name is invalid."
            );
        }

        emails.put(
                employeeId,
                cleanName + "@company.local"
        );
    }

    public synchronized void deleteEmail(
            String employeeId) {

        validate(employeeId, "Employee ID");

        emails.remove(employeeId);
    }

    // ============================================================
    // OFFBOARDING
    // ============================================================

    /**
     * Disable an existing account.
     *
     * Important:
     * Disabling an account is NOT the same as deleting it.
     *
     * The account remains in the system with status DISABLED.
     */
    public synchronized void disableAccount(
            String employeeId) {

        validate(employeeId, "Employee ID");

        if (!employees.containsKey(employeeId)) {
            throw new RuntimeException(
                    "Cannot disable account. Employee not found: "
                            + employeeId
            );
        }

        boolean accountFound = false;

        for (Account account : accounts.values()) {

            if (employeeId.equals(account.getEmployeeId())) {

                accountFound = true;

                /*
                 * Idempotent behavior:
                 * If it is already disabled, don't fail.
                 */
                if (!"DISABLED".equalsIgnoreCase(
                        account.getStatus())) {

                    account.setStatus("DISABLED");
                }
            }
        }

        /*
         * This is a real failure:
         * an employee exists but has no account.
         */
        if (!accountFound) {
            throw new RuntimeException(
                    "Cannot disable account. No account exists for employee: "
                            + employeeId
            );
        }
    }

    /**
     * Deactivate the employee only after the previous
     * offboarding steps have succeeded.
     */
    public synchronized void deactivateEmployee(
            String employeeId) {

        validate(employeeId, "Employee ID");

        Employee employee =
                employees.get(employeeId);

        if (employee == null) {
            throw new RuntimeException(
                    "Cannot deactivate employee. Employee not found: "
                            + employeeId
            );
        }

        if ("INACTIVE".equalsIgnoreCase(
                employee.getStatus())) {

            return;
        }

        employee.setStatus("INACTIVE");
    }

    // ============================================================
    // STATE / QUERY METHODS
    // ============================================================

    public synchronized Map<String, Employee> getEmployees() {

        return new LinkedHashMap<>(employees);
    }

    public synchronized Map<String, Account> getAccounts() {

        return new LinkedHashMap<>(accounts);
    }

    public synchronized List<ApplicationAccess>
    getApplicationAccess() {

        return new ArrayList<>(applicationAccess);
    }

    public synchronized Map<String, Resource>
    getResources() {

        return new LinkedHashMap<>(resources);
    }

    public synchronized Map<String, String> getEmails() {

        return new LinkedHashMap<>(emails);
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private void validate(
            String value,
            String fieldName) {

        if (value == null || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " cannot be empty."
            );
        }
    }
}