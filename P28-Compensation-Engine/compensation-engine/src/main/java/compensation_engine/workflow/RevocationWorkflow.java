package compensation_engine.workflow;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;

public class RevocationWorkflow {

    public static Saga create() {

        Saga saga = new Saga(WorkflowPolicy.KEEP_PARTIAL_COMPLETION);

        saga.addStep(new SagaStep(
                "Remove Access",
                () -> System.out.println("Access removed"),
                () -> System.out.println("DO NOT restore access")
        ));

        saga.addStep(new SagaStep(
                "Remove Permissions",
                () -> System.out.println("Permissions removed"),
                () -> System.out.println("DO NOT restore permissions")
        ));

        saga.addStep(new SagaStep(
                "Remove Account",
                () -> {
                    System.out.println("Removing account...");
                    throw new RuntimeException("Account removal failed");
                },
                () -> System.out.println("Account restoration is NOT allowed")
        ));

        return saga;
    }
}