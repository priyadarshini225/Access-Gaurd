package compensation_engine.tools;

import compensation_engine.model.Account;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.model.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessManagementTools {

    private final Map<String, Employee> employees = new HashMap<>();
    private final Map<String, Account> accounts = new HashMap<>();
    private final List<ApplicationAccess> applicationAccess = new ArrayList<>();
    private final Map<String, Resource> resources = new HashMap<>();

    // =========================
    // EMPLOYEE
    // =========================

    public Employee createEmployee(
            String employeeId,
            String name,
            String department,
            String role) {

        Employee employee = new Employee(
                employeeId,
                name,
                department,
                role,
                "ACTIVE"
        );

        employees.put(employeeId, employee);

        System.out.println("✓ Employee created: " + name);

        return employee;
    }

    public void deleteEmployee(String employeeId) {

        employees.remove(employeeId);

        System.out.println("↩ Employee deleted: " + employeeId);
    }

    // =========================
    // ACCOUNT
    // =========================

    public Account createAccount(
            String employeeId,
            String username) {

        String accountId = "ACC-" + employeeId;

        Account account = new Account(
                accountId,
                employeeId,
                username,
                "ACTIVE"
        );

        accounts.put(accountId, account);

        System.out.println("✓ Account created: " + username);

        return account;
    }

    public void deleteAccount(String employeeId) {

        accounts.values().removeIf(
                account ->
                        account.getEmployeeId().equals(employeeId)
        );

        System.out.println("↩ Account deleted: " + employeeId);
    }

    // =========================
    // APPLICATION ACCESS
    // =========================

    public ApplicationAccess grantApplicationAccess(
            String employeeId,
            String application,
            String accessLevel) {

        ApplicationAccess access =
                new ApplicationAccess(
                        employeeId,
                        application,
                        accessLevel,
                        "ACTIVE"
                );

        applicationAccess.add(access);

        System.out.println(
                "✓ " + application +
                " access granted to " + employeeId
        );

        return access;
    }

    public void removeApplicationAccess(
            String employeeId,
            String application) {

        applicationAccess.removeIf(
                access ->
                        access.getEmployeeId().equals(employeeId)
                        &&
                        access.getApplication().equals(application)
        );

        System.out.println(
                "↩ " + application +
                " access removed from " + employeeId
        );
    }

    // =========================
    // RESOURCE
    // =========================

    public Resource createResource(
            String employeeId,
            String resourceType) {

        String resourceId =
                "RES-" + employeeId + "-" + resourceType;

        Resource resource =
                new Resource(
                        resourceId,
                        employeeId,
                        resourceType,
                        "ACTIVE"
                );

        resources.put(resourceId, resource);

        System.out.println(
                "✓ Resource created: " + resourceType
        );

        return resource;
    }

    public void deleteResource(String employeeId) {

        resources.values().removeIf(
                resource ->
                        resource.getEmployeeId().equals(employeeId)
        );

        System.out.println(
                "↩ Resources deleted: " + employeeId
        );
    }

    // =========================
    // EMAIL
    // =========================

    public void createEmail(String employeeId) {

        System.out.println(
                "✓ Email created: "
                        + employeeId
                        + "@company.local"
        );
    }

    public void deleteEmail(String employeeId) {

        System.out.println(
                "↩ Email deleted: " + employeeId
        );
    }

    // =========================
    // OFFBOARDING
    // =========================

    public void disableAccount(String employeeId) {

        for (Account account : accounts.values()) {

            if (account.getEmployeeId().equals(employeeId)) {

                account.setStatus("DISABLED");

                System.out.println(
                        "✓ Account disabled: " + employeeId
                );

                return;
            }
        }

        throw new RuntimeException("Account not found");
    }

    public void deactivateEmployee(String employeeId) {

        Employee employee = employees.get(employeeId);

        if (employee != null) {

            employee.setStatus("INACTIVE");

            System.out.println(
                    "✓ Employee deactivated: " + employeeId
            );
        }
    }

    // =========================
    // GET CURRENT STATE
    // =========================

    public Map<String, Employee> getEmployees() {
        return employees;
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public List<ApplicationAccess> getApplicationAccess() {
        return applicationAccess;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }
}