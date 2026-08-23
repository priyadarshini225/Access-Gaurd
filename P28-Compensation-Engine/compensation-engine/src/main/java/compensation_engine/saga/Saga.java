package compensation_engine.saga;

import java.util.ArrayList;
import java.util.List;

public class Saga {

    private final List<SagaStep> steps = new ArrayList<>();
    private final WorkflowPolicy policy;
    private String residual;

    public Saga(WorkflowPolicy policy) {
        this.policy = policy;
    }

    public void addStep(SagaStep step) {
        steps.add(step);
    }

    public String getResidual() {
        return residual;
    }

    public void execute() {
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            try {
                System.out.println("Executing: " + step.getName());

                step.getAction().run();

                completedSteps.add(step);

            } catch (Exception e) {
                System.out.println("FAILED: " + step.getName());

                if (policy == WorkflowPolicy.ROLLBACK_ON_FAILURE) {
                    compensate(completedSteps);
                } else {
                    residual = step.getName();
                    System.out.println("Partial completion preserved.");
                    System.out.println("Residual: " + residual);
                }

                throw e;
            }
        }
    }

    private void compensate(List<SagaStep> completedSteps) {

        System.out.println("Starting compensation...");

        for (int i = completedSteps.size() - 1; i >= 0; i--) {

            SagaStep step = completedSteps.get(i);

            System.out.println("Compensating: " + step.getName());

            step.getCompensator().compensate();
        }
    }
}