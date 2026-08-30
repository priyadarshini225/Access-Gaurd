package compensation_engine.workflow;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaResult;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.tools.AccessManagementTools;

public class RevocationWorkflow {

    private final AccessManagementTools tools;

    public RevocationWorkflow(AccessManagementTools tools) {
        this.tools = tools;
    }

    public SagaResult run(
            String employeeId,
            String application,
            String failAt) {

        validate(employeeId, "Employee ID");
        validate(application, "Application");

        /*
         * IMPORTANT:
         *
         * Offboarding uses KEEP_PARTIAL_COMPLETION.
         *
         * We NEVER automatically compensate a successful
         * revocation step because compensation could restore
         * access that was intentionally removed.
         */
        Saga saga =
                new Saga(WorkflowPolicy.KEEP_PARTIAL_COMPLETION);

        /*
         * STEP 1
         * Remove application access
         */
        saga.addStep(
                new SagaStep(
                        "Remove " + application + " Access",

                        () -> {
                            failIf(
                                    failAt,
                                    "Remove Application Access"
                            );

                            tools.removeApplicationAccess(
                                    employeeId,
                                    application
                            );
                        },

                        /*
                         * DO NOT restore application access.
                         */
                        () -> {
                            throw new RuntimeException(
                                    "Unsafe compensation blocked: " +
                                    "restoring revoked application access " +
                                    "is not permitted."
                            );
                        }
                )
        );

        /*
         * STEP 2
         * Disable account
         */
        saga.addStep(
                new SagaStep(
                        "Disable Account",

                        () -> {
                            failIf(
                                    failAt,
                                    "Disable Account"
                            );

                            tools.disableAccount(
                                    employeeId
                            );
                        },

                        /*
                         * DO NOT re-enable account automatically.
                         */
                        () -> {
                            throw new RuntimeException(
                                    "Unsafe compensation blocked: " +
                                    "re-enabling a disabled account " +
                                    "is not permitted."
                            );
                        }
                )
        );

        /*
         * STEP 3
         * Revoke resources
         */
        saga.addStep(
                new SagaStep(
                        "Revoke Resources",

                        () -> {
                            failIf(
                                    failAt,
                                    "Revoke Resources"
                            );

                            tools.deleteResource(
                                    employeeId
                            );
                        },

                        /*
                         * DO NOT recreate revoked resources.
                         */
                        () -> {
                            throw new RuntimeException(
                                    "Unsafe compensation blocked: " +
                                    "recreating revoked resources " +
                                    "is not permitted."
                            );
                        }
                )
        );

        /*
         * STEP 4
         * Deactivate employee
         */
        saga.addStep(
                new SagaStep(
                        "Deactivate Employee",

                        () -> {
                            failIf(
                                    failAt,
                                    "Deactivate Employee"
                            );

                            tools.deactivateEmployee(
                                    employeeId
                            );
                        },

                        /*
                         * DO NOT reactivate employee automatically.
                         */
                        () -> {
                            throw new RuntimeException(
                                    "Unsafe compensation blocked: " +
                                    "reactivating an offboarded employee " +
                                    "is not permitted."
                            );
                        }
                )
        );

        return saga.execute();
    }

    private void failIf(
            String failAt,
            String step) {

        if (failAt == null || failAt.isBlank()) {
            return;
        }

        if (failAt.equalsIgnoreCase(step)) {
            throw new RuntimeException(
                    "Simulated failure at: " + step
            );
        }
    }

    private void validate(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be empty."
            );
        }
    }
}
