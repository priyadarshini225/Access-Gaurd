package compensation_engine.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SagaResult {

    private String workflowId;
    private String workflowType;
    private String employeeId;
    private String status;
    private String message;
    private String failedStep;
    private String failureReason;
    private String residual;
    private String executedAt;
    private Long durationMs;

    /** Populated only when a real DataInconsistencyException is detected. */
    private String incidentReport;

    private final List<Map<String, String>> steps = new ArrayList<>();
    private final List<Map<String, String>> compensation = new ArrayList<>();

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getWorkflowType() { return workflowType; }
    public void setWorkflowType(String workflowType) { this.workflowType = workflowType; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFailedStep() { return failedStep; }
    public void setFailedStep(String failedStep) { this.failedStep = failedStep; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getResidual() { return residual; }
    public void setResidual(String residual) { this.residual = residual; }

    public String getExecutedAt() { return executedAt; }
    public void setExecutedAt(String executedAt) { this.executedAt = executedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getIncidentReport() { return incidentReport; }
    public void setIncidentReport(String incidentReport) { this.incidentReport = incidentReport; }

    public List<Map<String, String>> getSteps() { return steps; }
    public List<Map<String, String>> getCompensation() { return compensation; }
}
