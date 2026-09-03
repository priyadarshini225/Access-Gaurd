package compensation_engine.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Saga {

    private static final Logger log = LoggerFactory.getLogger(Saga.class);

    private final List<SagaStep> steps = new ArrayList<>();
    private final WorkflowPolicy policy;

    public Saga(WorkflowPolicy policy) {
        this.policy = policy;
    }

    public void addStep(SagaStep step) {
        if (step == null) {
            throw new IllegalArgumentException("Saga step cannot be null.");
        }
        steps.add(step);
    }

    public SagaResult execute() {
        SagaResult result = new SagaResult();
        List<SagaStep> completedSteps = new ArrayList<>();

        log.info("Starting Saga execution with policy: {} across {} steps", policy, steps.size());

        for (SagaStep step : steps) {
            try {
                log.info("Executing step: '{}'", step.getName());
                step.getAction().run();
                completedSteps.add(step);
                result.getSteps().add(item(step.getName(), "SUCCESS"));
                log.info("Step '{}' succeeded", step.getName());
            } catch (Exception ex) {
                log.error("Step '{}' failed: {}", step.getName(), ex.getMessage(), ex);
                result.setFailedStep(step.getName());
                result.setFailureReason(ex.getMessage() != null ? ex.getMessage() : ex.toString());
                result.getSteps().add(item(step.getName(), "FAILED"));

                /*
                 * ONBOARDING / PROVISIONING
                 * Safe to compensate: undo previous completed steps in reverse order.
                 */
                if (policy == WorkflowPolicy.ROLLBACK_ON_FAILURE) {
                    result.setStatus("ROLLED_BACK");
                    result.setMessage(
                            "Workflow failed at '" + step.getName() + "'. " +
                            "Successful previous steps are being compensated in reverse order."
                    );

                    log.warn("Initiating reverse-order compensation for {} completed steps", completedSteps.size());

                    for (int i = completedSteps.size() - 1; i >= 0; i--) {
                        SagaStep completedStep = completedSteps.get(i);
                        try {
                            log.info("Compensating step: '{}'", completedStep.getName());
                            completedStep.getCompensator().compensate();
                            result.getCompensation().add(item("Undo " + completedStep.getName(), "SUCCESS"));
                            log.info("Successfully compensated '{}'", completedStep.getName());
                        } catch (Exception compensationError) {
                            log.error("Failed to compensate '{}': {}", completedStep.getName(), compensationError.getMessage(), compensationError);
                            result.getCompensation().add(item("Undo " + completedStep.getName(), "FAILED"));
                            result.setStatus("PARTIAL_COMPENSATION");
                            result.setResidual(completedStep.getName());
                            result.setMessage(
                                    "Workflow failed and one or more compensation operations also failed. " +
                                    "Human intervention is required."
                            );
                        }
                    }
                    return result;
                }

                /*
                 * OFFBOARDING / REVOCATION
                 * Security safety rule: NEVER automatically restore revoked access or reactivate accounts.
                 */
                if (policy == WorkflowPolicy.KEEP_PARTIAL_COMPLETION) {
                    result.setStatus("PARTIAL");
                    result.setResidual(step.getName());
                    result.setMessage(
                            "Offboarding stopped at '" + step.getName() + "'. " +
                            "Previously completed revocation steps were preserved. " +
                            "Automatic rollback is blocked for security reasons."
                    );
                    log.warn("Offboarding stopped at '{}'. Preserved partial completion under security policy.", step.getName());
                    return result;
                }

                result.setStatus("FAILED");
                result.setMessage("Workflow failed at: " + step.getName());
                return result;
            }
        }

        result.setStatus("SUCCESS");
        result.setMessage("All workflow steps completed successfully.");
        log.info("Saga execution completed successfully");
        return result;
    }

    private Map<String, String> item(String name, String status) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("status", status);
        return result;
    }
}
