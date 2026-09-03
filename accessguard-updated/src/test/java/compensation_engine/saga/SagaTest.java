package compensation_engine.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SagaTest {

    @Test
    @DisplayName("Saga should execute all steps successfully")
    void testSuccessfulExecution() {
        Saga saga = new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);
        List<String> executed = new ArrayList<>();

        saga.addStep(new SagaStep("Step1", () -> executed.add("1"), () -> executed.remove("1")));
        saga.addStep(new SagaStep("Step2", () -> executed.add("2"), () -> executed.remove("2")));

        SagaResult result = saga.execute();

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(2, executed.size());
        assertEquals("1", executed.get(0));
        assertEquals("2", executed.get(1));
        assertEquals(2, result.getSteps().size());
        assertTrue(result.getCompensation().isEmpty());
    }

    @Test
    @DisplayName("Saga with ROLLBACK_ON_FAILURE should compensate in reverse order on failure")
    void testReverseOrderRollbackOnFailure() {
        Saga saga = new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);
        List<String> actions = new ArrayList<>();
        List<String> compensations = new ArrayList<>();

        saga.addStep(new SagaStep("Step1", () -> actions.add("A"), () -> compensations.add("Undo A")));
        saga.addStep(new SagaStep("Step2", () -> actions.add("B"), () -> compensations.add("Undo B")));
        saga.addStep(new SagaStep("Step3", () -> { throw new RuntimeException("Failure at C"); }, () -> compensations.add("Undo C")));

        SagaResult result = saga.execute();

        assertEquals("ROLLED_BACK", result.getStatus());
        assertEquals("Step3", result.getFailedStep());
        assertEquals("Failure at C", result.getFailureReason());

        // Actions executed
        assertEquals(List.of("A", "B"), actions);

        // Compensations must be in REVERSE order: Undo B, then Undo A
        assertEquals(List.of("Undo B", "Undo A"), compensations);
        assertEquals(2, result.getCompensation().size());
        assertEquals("Undo Step2", result.getCompensation().get(0).get("name"));
        assertEquals("SUCCESS", result.getCompensation().get(0).get("status"));
        assertEquals("Undo Step1", result.getCompensation().get(1).get("name"));
        assertEquals("SUCCESS", result.getCompensation().get(1).get("status"));
    }

    @Test
    @DisplayName("Saga with KEEP_PARTIAL_COMPLETION should NOT roll back previous steps on failure")
    void testKeepPartialCompletionPolicy() {
        Saga saga = new Saga(WorkflowPolicy.KEEP_PARTIAL_COMPLETION);
        List<String> actions = new ArrayList<>();
        List<String> compensations = new ArrayList<>();

        saga.addStep(new SagaStep("Revoke App", () -> actions.add("AppRevoked"), () -> compensations.add("AppRestored")));
        saga.addStep(new SagaStep("Disable Account", () -> { throw new RuntimeException("Account DB down"); }, () -> compensations.add("AccountReenabled")));

        SagaResult result = saga.execute();

        assertEquals("PARTIAL", result.getStatus());
        assertEquals("Disable Account", result.getResidual());
        assertEquals(List.of("AppRevoked"), actions);
        // Compensation should NOT be called
        assertTrue(compensations.isEmpty());
        assertTrue(result.getCompensation().isEmpty());
    }

    @Test
    @DisplayName("Saga should set PARTIAL_COMPENSATION if a compensation step fails")
    void testPartialCompensationWhenCompensatorFails() {
        Saga saga = new Saga(WorkflowPolicy.ROLLBACK_ON_FAILURE);

        saga.addStep(new SagaStep("Step1", () -> {}, () -> { throw new RuntimeException("Compensator broke"); }));
        saga.addStep(new SagaStep("Step2", () -> { throw new RuntimeException("Fail at 2"); }, () -> {}));

        SagaResult result = saga.execute();

        assertEquals("PARTIAL_COMPENSATION", result.getStatus());
        assertEquals("Step1", result.getResidual());
        assertEquals(1, result.getCompensation().size());
        assertEquals("FAILED", result.getCompensation().get(0).get("status"));
    }
}
