package compensation_engine.saga;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Saga {

    private final List<SagaStep> steps = new ArrayList<>();

    private final WorkflowPolicy policy;

    public Saga(WorkflowPolicy policy) {
        this.policy = policy;
    }

    public void addStep(SagaStep step) {
        if (step == null) {
            throw new IllegalArgumentException(
                    "Saga step cannot be null."
            );
        }

        steps.add(step);
    }

    public SagaResult execute() {

        SagaResult result = new SagaResult();

        /*
         * Only successfully completed steps are stored here.
         *
         * This is extremely important because we should
         * compensate ONLY operations that actually happened.
         */
        List<SagaStep> completedSteps =
                new ArrayList<>();

        for (SagaStep step : steps) {

            try {

                /*
                 * Execute the actual business operation.
                 */
                step.getAction().run();

                /*
                 * Add the step only AFTER successful execution.
                 */
                completedSteps.add(step);

                result.getSteps().add(
                        item(
                                step.getName(),
                                "SUCCESS"
                        )
                );

            } catch (Exception ex) {

                /*
                 * The current step failed.
                 */
                result.setFailedStep(
                        step.getName()
                );

                result.getSteps().add(
                        item(
                                step.getName(),
                                "FAILED"
                        )
                );

                /*
                 * =================================================
                 * ONBOARDING / PROVISIONING
                 * =================================================
                 *
                 * Safe to compensate because onboarding resources
                 * should not remain when provisioning fails.
                 */
                if (policy ==
                        WorkflowPolicy.ROLLBACK_ON_FAILURE) {

                    result.setStatus("ROLLED_BACK");

                    result.setMessage(
                            "Workflow failed at '" +
                            step.getName() +
                            "'. Successful previous steps " +
                            "are being compensated in reverse order."
                    );

                    /*
                     * Reverse-order compensation.
                     *
                     * Example:
                     *
                     * A → B → C → FAILURE
                     *
                     * Compensation:
                     *
                     * Undo B
                     * Undo A
                     */
                    for (
                            int i = completedSteps.size() - 1;
                            i >= 0;
                            i--
                    ) {

                        SagaStep completedStep =
                                completedSteps.get(i);

                        try {

                            completedStep
                                    .getCompensator()
                                    .compensate();

                            result.getCompensation().add(
                                    item(
                                            "Undo " +
                                            completedStep.getName(),
                                            "SUCCESS"
                                    )
                            );

                        } catch (Exception compensationError) {

                            result.getCompensation().add(
                                    item(
                                            "Undo " +
                                            completedStep.getName(),
                                            "FAILED"
                                    )
                            );

                            /*
                             * If compensation itself fails,
                             * residual state may remain.
                             */
                            result.setStatus(
                                    "PARTIAL_COMPENSATION"
                            );

                            result.setResidual(
                                    completedStep.getName()
                            );

                            result.setMessage(
                                    "Workflow failed and one or more " +
                                    "compensation operations also failed. " +
                                    "Human intervention is required."
                            );
                        }
                    }

                    return result;
                }

                /*
                 * =================================================
                 * OFFBOARDING / REVOCATION
                 * =================================================
                 *
                 * Never automatically restore access.
                 */
                if (policy ==
                        WorkflowPolicy.KEEP_PARTIAL_COMPLETION) {

                    result.setStatus("PARTIAL");

                    result.setResidual(
                            step.getName()
                    );

                    result.setMessage(
                            "Offboarding stopped at '" +
                            step.getName() +
                            "'. Previously completed revocation " +
                            "steps were preserved. Automatic rollback " +
                            "is blocked for security reasons."
                    );

                    return result;
                }

                /*
                 * Defensive fallback.
                 */
                result.setStatus("FAILED");

                result.setMessage(
                        "Workflow failed at: " +
                        step.getName()
                );

                return result;
            }
        }

        /*
         * Every step completed.
         */
        result.setStatus("SUCCESS");

        result.setMessage(
                "All workflow steps completed successfully."
        );

        return result;
    }

    private Map<String, String> item(
            String name,
            String status) {

        Map<String, String> result =
                new LinkedHashMap<>();

        result.put("name", name);
        result.put("status", status);

        return result;
    }
}
