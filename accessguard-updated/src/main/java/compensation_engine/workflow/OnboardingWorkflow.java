package compensation_engine.workflow;

import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.exception.DataInconsistencyException;
import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaResult;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.service.*;

import java.util.List;

public class OnboardingWorkflow {

    private final EmployeeService employeeService;
    private final AccountService accountService;
    private final EmailService emailService;
    private final ApplicationAccessConnectorRegistry accessConnectorRegistry;
    private final ResourceService resourceService;

    public OnboardingWorkflow(EmployeeService employeeService,
                              AccountService accountService,
                              EmailService emailService,
                              ResourceService resourceService,
                              ApplicationAccessConnectorRegistry accessConnectorRegistry) {
        this.employeeService = employeeService;
        this.accountService = accountService;
        this.emailService = emailService;
        this.resourceService = resourceService;
        this.accessConnectorRegistry = accessConnectorRegistry;
    }

    public SagaResult run(String employeeId,
                          String name,
                          String department,
                          String role,
                          String application,
                          String accessLevel,
                          String failAt) {

                return run(employeeId, name, department, role,
                    List.of(new AccessGrantRequest(application, accessLevel)), failAt);
                }

                public SagaResult run(String employeeId,
                          String name,
                          String department,
                          String role,
                          List<AccessGrantRequest> accessRequests,
                          String failAt) {

        validate(employeeId, "Employee ID");
        validate(name, "Employee name");
        validate(department, "Department");
        validate(role, "Role");
        if (accessRequests == null || accessRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one access request is required.");
        }

        Saga saga = new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);

        /*
         * STEP 1: Create Employee
         */
        saga.addStep(new SagaStep(
                "Create Employee",
                () -> {
                    failIf(failAt, "Create Employee");
                    employeeService.createEmployee(employeeId, name, department, role);
                },
                () -> employeeService.deleteEmployee(employeeId)
        ));

        /*
         * STEP 2: Create Account
         */
        saga.addStep(new SagaStep(
                "Create Account",
                () -> {
                    failIf(failAt, "Create Account");
                    String username = createUsername(name);
                    accountService.createAccount(employeeId, username);
                },
                () -> safeRun(() -> accountService.deleteAccount(employeeId))
        ));

        /*
         * STEP 3: Create Email
         */
        saga.addStep(new SagaStep(
                "Create Email",
                () -> {
                    failIf(failAt, "Create Email");
                    emailService.createEmail(employeeId);
                },
                () -> safeRun(() -> emailService.deleteEmail(employeeId))
        ));

        /*
         * STEP 4: Grant Application Access
         */
        for (AccessGrantRequest accessRequest : accessRequests) {
            validate(accessRequest.getApplication(), "Application");
            validate(accessRequest.getAccessLevel(), "Access level");

            String application = accessRequest.getApplication().trim();
            String accessLevel = accessRequest.getAccessLevel().trim();
            String stepName = "Grant " + application + " Access";

            saga.addStep(new SagaStep(
                    stepName,
                    () -> {
                        failIf(failAt, "Grant Application Access");
                        failIf(failAt, stepName);
                        accessConnectorRegistry.resolve(application)
                            .grantAccess(employeeId, application, accessLevel, accessRequest.getExpiresAt());
                    },
                        () -> safeRun(() -> accessConnectorRegistry.resolve(application)
                            .revokeAccess(employeeId, application))
            ));
        }

        /*
         * STEP 5: Create Storage
         */
        saga.addStep(new SagaStep(
                "Create Storage",
                () -> {
                    failIf(failAt, "Create Storage");
                    resourceService.createResource(employeeId, "Cloud Storage");
                },
                () -> safeRun(() -> resourceService.deleteResources(employeeId))
        ));

        SagaResult result = saga.execute();
        result.setEmployeeId(employeeId);
        return result;
    }

    private void failIf(String failAt, String step) {
        if (failAt == null || failAt.isBlank()) {
            return;
        }

        if (failAt.equalsIgnoreCase(step)
                || failAt.equalsIgnoreCase(normalizeStepName(step))) {
            throw new RuntimeException("Simulated failure at: " + step);
        }
    }

    /**
     * During ONBOARDING ROLLBACK: if a record doesn't exist it means the forward
     * step never ran — silently skip. DataInconsistencyException is only an
     * error during offboarding (where the record MUST exist).
     */
    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (DataInconsistencyException ignored) {
            // Expected during rollback — forward step never completed
        }
    }

    private String normalizeStepName(String step) {
        return step.replace("Grant ", "")
                .replace(" Access", "")
                .trim();
    }

    private String createUsername(String name) {
        String username = name.toLowerCase()
                .trim()
                .replaceAll("\\s+", ".");

        username = username.replaceAll("[^a-z0-9.]", "");

        if (username.isBlank()) {
            throw new IllegalArgumentException("Unable to create username from employee name.");
        }

        return username;
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}