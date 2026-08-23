package compensation_engine.workflow;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.tools.AccessManagementTools;

public class OnboardingWorkflow {

    private final AccessManagementTools tools;

    public OnboardingWorkflow(AccessManagementTools tools) {
        this.tools = tools;
    }

    public void onboard(
            String employeeId,
            String name,
            String department,
            String role,
            boolean failAtStorage) {

        // ONBOARDING = SAFE TO ROLLBACK
        WorkflowPolicy policy = WorkflowPolicy.ROLLBACK_ON_FAILURE;

        Saga saga = new Saga(policy);

        // STEP 1
        saga.addStep(
                new SagaStep(
                        "Create Employee",
                        () -> tools.createEmployee(
                                employeeId,
                                name,
                                department,
                                role
                        ),
                        () -> tools.deleteEmployee(employeeId)
                )
        );

        // STEP 2
        saga.addStep(
                new SagaStep(
                        "Create Account",
                        () -> tools.createAccount(
                                employeeId,
                                name.toLowerCase()
                        ),
                        () -> tools.deleteAccount(employeeId)
                )
        );

        // STEP 3
        saga.addStep(
                new SagaStep(
                        "Create Email",
                        () -> tools.createEmail(employeeId),
                        () -> tools.deleteEmail(employeeId)
                )
        );

        // STEP 4
        saga.addStep(
                new SagaStep(
                        "Grant GitLab Access",
                        () -> tools.grantApplicationAccess(
                                employeeId,
                                "GitLab",
                                "Developer"
                        ),
                        () -> tools.removeApplicationAccess(
                                employeeId,
                                "GitLab"
                        )
                )
        );

        // STEP 5
        saga.addStep(
                new SagaStep(
                        "Create Storage",
                        () -> {
                            if (failAtStorage) {
                                throw new RuntimeException(
                                        "Storage service unavailable"
                                );
                            }

                            tools.createResource(
                                    employeeId,
                                    "Cloud Storage"
                            );
                        },
                        () -> tools.deleteResource(employeeId)
                )
        );

        saga.execute();
    }
}