package compensation_engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persists Saga execution history so workflow results survive
 * application restarts.
 */
@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @Column(name = "workflow_id", nullable = false, unique = true)
    private String workflowId;

    @Column(name = "workflow_type", nullable = false)
    private String workflowType;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(nullable = false)
    private String status;

    @Column(name = "failed_step")
    private String failedStep;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(length = 5000)
    private String summary;

    @Column(name = "executed_at", nullable = false)
    private String executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public WorkflowExecution() {}

    public WorkflowExecution(String workflowId, String workflowType,
                              String employeeId, String status,
                              String failedStep, String failureReason,
                              String summary, String executedAt,
                              Long durationMs) {
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.employeeId = employeeId;
        this.status = status;
        this.failedStep = failedStep;
        this.failureReason = failureReason;
        this.summary = summary;
        this.executedAt = executedAt;
        this.durationMs = durationMs;
    }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailedStep() { return failedStep; }
    public void setFailedStep(String failedStep) { this.failedStep = failedStep; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getExecutedAt() { return executedAt; }
    public void setExecutedAt(String executedAt) { this.executedAt = executedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
