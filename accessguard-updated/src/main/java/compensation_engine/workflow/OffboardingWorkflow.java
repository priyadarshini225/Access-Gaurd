package compensation_engine.workflow;

import compensation_engine.exception.DataInconsistencyException;
import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaResult;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.service.*;

/**
 * Complete Employee Offboarding Workflow.
 *
 * Steps:
 * 1. Remove all application access
 * 2. Disable account
 * 3. Revoke resources (Storage)
 * 4. Revoke email
 * 5. Deactivate employee
 *
 * Safety Policy: KEEP_PARTIAL_COMPLETION.
 * We never restore access automatically on failure.
 *
 * Real Failure: If any record is missing in the database (because it was
 * deleted externally), a DataInconsistencyException is thrown by the service.
 * The Saga catches it, marks the workflow FAILED, and attaches an incident
 * report that the administrator must act on.
 */
public class OffboardingWorkflow {

    private final EmployeeService employeeService;
    private final AccountService accountService;
    private final EmailService emailService;
    private final ApplicationAccessConnectorRegistry accessConnectorRegistry;
    private final ResourceService resourceService;

    public OffboardingWorkflow(EmployeeService employeeService,
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

    public SagaResult run(String employeeId, String application, String failAt) {
        validate(employeeId, "Employee ID");

        Saga saga = new Saga(WorkflowPolicy.KEEP_PARTIAL_COMPLETION);

        /*
         * STEP 1: Remove Application Access
         */
        String appStepName = (application != null && !application.isBlank())
                ? "Remove " + application + " Access"
                : "Remove Application Access";

        saga.addStep(new SagaStep(
                appStepName,
                () -> {
                    failIf(failAt, "Remove Application Access");
                    failIf(failAt, appStepName);
                    if (application != null && !application.isBlank()) {
                        accessConnectorRegistry.resolve(application)
                            .revokeAccess(employeeId, application);
                    } else {
                        accessConnectorRegistry.resolve("all")
                            .revokeAllAccess(employeeId);
                    }
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: restoring revoked application access is not permitted."
                    );
                }
        ));

        /*
         * STEP 2: Disable Account
         */
        saga.addStep(new SagaStep(
                "Disable Account",
                () -> {
                    failIf(failAt, "Disable Account");
                    accountService.disableAccount(employeeId);
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: re-enabling a disabled account is not permitted."
                    );
                }
        ));

        /*
         * STEP 3: Revoke Resources
         */
        saga.addStep(new SagaStep(
                "Revoke Resources",
                () -> {
                    failIf(failAt, "Revoke Resources");
                    resourceService.deleteResources(employeeId);
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: recreating revoked resources is not permitted."
                    );
                }
        ));

        /*
         * STEP 4: Remove Email
         */
        saga.addStep(new SagaStep(
                "Remove Email",
                () -> {
                    failIf(failAt, "Remove Email");
                    emailService.deleteEmail(employeeId);
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: restoring deleted email is not permitted."
                    );
                }
        ));

        /*
         * STEP 5: Deactivate Employee
         */
        saga.addStep(new SagaStep(
                "Deactivate Employee",
                () -> {
                    failIf(failAt, "Deactivate Employee");
                    employeeService.deactivateEmployee(employeeId);
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: reactivating an offboarded employee is not permitted."
                    );
                }
        ));

        SagaResult result = saga.execute();

        // Attach incident report if a data inconsistency caused the failure
        attachIncidentReportIfNeeded(result, employeeId);

        return result;
    }

    /**
     * If the workflow failed because of a DataInconsistencyException,
     * populate a human-readable incident report on the result so the
     * UI can display it prominently to the administrator.
     */
    private void attachIncidentReportIfNeeded(SagaResult result, String employeeId) {
        if (!"FAILED".equals(result.getStatus())) {
            return;
        }

        String reason = result.getFailureReason();
        if (reason == null || !reason.contains("DATA INCONSISTENCY")) {
            return;
        }

        String report = String.format(
            "INCIDENT REPORT\n" +
            "═══════════════════════════════════════════════════\n" +
            "Employee ID   : %s\n" +
            "Failed Step   : %s\n" +
            "Root Cause    : A database record was missing at the time offboarding ran.\n" +
            "               This record should have existed. Its absence indicates it\n" +
            "               was deleted externally — either by mistake, a bug, or a\n" +
            "               manual intervention without following procedure.\n\n" +
            "Steps completed before failure are NOT reversed (security policy).\n\n" +
            "ADMINISTRATOR ACTIONS REQUIRED:\n" +
            "  1. Identify who or what deleted the missing record.\n" +
            "  2. Check audit logs for unexpected deletions.\n" +
            "  3. Verify all remaining offboarding steps manually:\n" +
            "     - Is application access revoked?\n" +
            "     - Is account disabled?\n" +
            "     - Are cloud resources deleted?\n" +
            "     - Is email removed?\n" +
            "     - Is employee marked inactive?\n" +
            "  4. Complete any remaining steps manually in the system.\n" +
            "  5. File a security incident report.\n" +
            "═══════════════════════════════════════════════════",
            employeeId,
            result.getFailedStep()
        );

        result.setIncidentReport(report);
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }

    private void failIf(String failAt, String step) {
        if (failAt != null && !failAt.isBlank() && failAt.equalsIgnoreCase(step)) {
            throw new RuntimeException("Simulated failure at: " + step);
        }
    }
}
