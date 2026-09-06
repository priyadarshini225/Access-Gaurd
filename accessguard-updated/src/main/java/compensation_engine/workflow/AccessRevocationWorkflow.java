package compensation_engine.workflow;

import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaResult;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.service.AccessService;

/**
 * Application Access Revocation Workflow.
 *
 * Scope:
 * - Revokes access ONLY for the specified application.
 * - Does NOT disable the account.
 * - Does NOT deactivate the employee.
 * - Does NOT remove emails or storage.
 *
 * Safety Policy: KEEP_PARTIAL_COMPLETION (never restore revoked access automatically on error).
 */
public class AccessRevocationWorkflow {

    private final ApplicationAccessConnectorRegistry accessConnectorRegistry;

    public AccessRevocationWorkflow(ApplicationAccessConnectorRegistry accessConnectorRegistry) {
        this.accessConnectorRegistry = accessConnectorRegistry;
    }

    public SagaResult run(String employeeId, String application, String failAt) {
        validate(employeeId, "Employee ID");
        validate(application, "Application");

        Saga saga = new Saga(WorkflowPolicy.KEEP_PARTIAL_COMPLETION);

        String stepName = "Revoke " + application + " Access";

        saga.addStep(new SagaStep(
                stepName,
                () -> {
                    failIf(failAt, "Revoke Application Access");
                    failIf(failAt, stepName);
                    failIf(failAt, "Remove " + application + " Access");
                        accessConnectorRegistry.resolve(application)
                            .revokeAccess(employeeId, application);
                },
                () -> {
                    throw new RuntimeException(
                            "Unsafe compensation blocked: restoring revoked application access is not permitted."
                    );
                }
        ));

        return saga.execute();
    }

    private void failIf(String failAt, String step) {
        if (failAt == null || failAt.isBlank()) {
            return;
        }

        if (failAt.equalsIgnoreCase(step)) {
            throw new RuntimeException("Simulated failure at: " + step);
        }
    }

    private void validate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}
