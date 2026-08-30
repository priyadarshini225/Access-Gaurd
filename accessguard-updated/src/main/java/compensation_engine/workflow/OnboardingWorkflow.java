package compensation_engine.workflow;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaResult;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.tools.AccessManagementTools;

public class OnboardingWorkflow {

    private final AccessManagementTools tools;

    public OnboardingWorkflow(AccessManagementTools tools) {
        this.tools = tools;
    }

    public SagaResult run(
            String employeeId,
            String name,
            String department,
            String role,
            String application,
            String accessLevel,
            String failAt) {

        validate(employeeId, "Employee ID");
        validate(name, "Employee name");
        validate(department, "Department");
        validate(role, "Role");
        validate(application, "Application");
        validate(accessLevel, "Access level");

        Saga saga =
                new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);

        /*
         * STEP 1
         * Create Employee
         */
        saga.addStep(
                new SagaStep(
                        "Create Employee",

                        () -> {
                            failIf(
                                    failAt,
                                    "Create Employee"
                            );

                            tools.createEmployee(
                                    employeeId,
                                    name,
                                    department,
                                    role
                            );
                        },

                        () -> tools.deleteEmployee(employeeId)
                )
        );

        /*
         * STEP 2
         * Create Account
         */
        saga.addStep(
                new SagaStep(
                        "Create Account",

                        () -> {
                            failIf(
                                    failAt,
                                    "Create Account"
                            );

                            String username =
                                    createUsername(name);

                            tools.createAccount(
                                    employeeId,
                                    username
                            );
                        },

                        () -> tools.deleteAccount(employeeId)
                )
        );

        /*
         * STEP 3
         * Create Email
         */
        saga.addStep(
                new SagaStep(
                        "Create Email",

                        () -> {
                            failIf(
                                    failAt,
                                    "Create Email"
                            );

                            tools.createEmail(employeeId);
                        },

                        () -> tools.deleteEmail(employeeId)
                )
        );

        /*
         * STEP 4
         * Grant Application Access
         */
        saga.addStep(
                new SagaStep(
                        "Grant " + application + " Access",

                        () -> {
                            failIf(
                                    failAt,
                                    "Grant Application Access"
                            );

                            tools.grantApplicationAccess(
                                    employeeId,
                                    application,
                                    accessLevel
                            );
                        },

                        () -> tools.removeApplicationAccess(
                                employeeId,
                                application
                        )
                )
        );

        /*
         * STEP 5
         * Create Storage
         */
        saga.addStep(
                new SagaStep(
                        "Create Storage",

                        () -> {
                            failIf(
                                    failAt,
                                    "Create Storage"
                            );

                            tools.createResource(
                                    employeeId,
                                    "Cloud Storage"
                            );
                        },

                        () -> tools.deleteResource(employeeId)
                )
        );

        return saga.execute();
    }

    /**
     * Simulates a failure only when the requested
     * failure point exactly matches the current step.
     *
     * Empty failAt means:
     * run normally without artificial failure.
     */
    private void failIf(
            String failAt,
            String step) {

        if (failAt == null || failAt.isBlank()) {
            return;
        }

        if (failAt.equalsIgnoreCase(step)
                || failAt.equalsIgnoreCase(
                normalizeStepName(step))) {

            throw new RuntimeException(
                    "Simulated failure at: " + step
            );
        }
    }

    private String normalizeStepName(String step) {

        return step
                .replace("Grant ", "")
                .replace(" Access", "")
                .trim();
    }

    /**
     * Creates a username from the employee's
     * actual name.
     */
    private String createUsername(String name) {

        String username =
                name.toLowerCase()
                        .trim()
                        .replaceAll("\\s+", ".");

        username =
                username.replaceAll(
                        "[^a-z0-9.]",
                        ""
                );

        if (username.isBlank()) {
            throw new IllegalArgumentException(
                    "Unable to create username from employee name."
            );
        }

        return username;
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