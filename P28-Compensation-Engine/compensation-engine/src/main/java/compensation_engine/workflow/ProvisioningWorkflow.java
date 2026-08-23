package compensation_engine.workflow;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;

public class ProvisioningWorkflow {

    public static Saga create() {

        Saga saga = new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);

        saga.addStep(new SagaStep(
                "Create User",
                () -> System.out.println("User created"),
                () -> System.out.println("User deleted")
        ));

        saga.addStep(new SagaStep(
                "Create Database",
                () -> System.out.println("Database created"),
                () -> System.out.println("Database deleted")
        ));

        saga.addStep(new SagaStep(
                "Create Storage",
                () -> System.out.println("Storage created"),
                () -> System.out.println("Storage deleted")
        ));

        saga.addStep(new SagaStep(
                "Configure Service",
                () -> {
                    System.out.println("Configuring service...");
                    throw new RuntimeException("Service configuration failed");
                },
                () -> System.out.println("Service configuration removed")
        ));

        return saga;
    }
}