package compensation_engine.service;

import compensation_engine.model.WorkflowExecution;
import compensation_engine.repository.WorkflowExecutionRepository;
import compensation_engine.saga.SagaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowExecutionService {

    private static final Logger log =
            LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowExecutionRepository executionRepository;

    public WorkflowExecutionService(WorkflowExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Transactional
    public WorkflowExecution recordExecution(String workflowType,
                                             String employeeId,
                                             SagaResult result,
                                             long durationMs) {

        String workflowId = result.getWorkflowId() != null && !result.getWorkflowId().isBlank()
                ? result.getWorkflowId()
                : UUID.randomUUID().toString();

        result.setWorkflowId(workflowId);
        result.setWorkflowType(workflowType);
        result.setEmployeeId(employeeId);
        result.setDurationMs(durationMs);
        result.setExecutedAt(Instant.now().toString());

        WorkflowExecution execution = new WorkflowExecution(
                workflowId,
                workflowType,
                employeeId,
                result.getStatus(),
                result.getFailedStep(),
                result.getFailureReason(),
                result.getMessage(),
                result.getExecutedAt(),
                durationMs
        );

        WorkflowExecution saved = executionRepository.save(execution);
        log.info("Recorded workflow execution: id={}, type={}, status={}, duration={}ms",
                workflowId, workflowType, result.getStatus(), durationMs);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecution> getRecentExecutions() {
        return executionRepository.findTop20ByOrderByExecutedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<WorkflowExecution> getExecutionsByEmployee(String employeeId) {
        return executionRepository.findByEmployeeIdOrderByExecutedAtDesc(employeeId);
    }
}
