package compensation_engine.agent.remediation;

import compensation_engine.agent.reconciliation.DriftSeverity;

public class RemediationTask {

    private String taskId;
    private String findingCode;
    private String employeeId;
    private String action;
    private DriftSeverity severity;
    private RemediationDisposition disposition;
    private String status;

    public RemediationTask() {}

    public RemediationTask(String taskId, String findingCode, String employeeId,
                           String action, DriftSeverity severity,
                           RemediationDisposition disposition, String status) {
        this.taskId = taskId;
        this.findingCode = findingCode;
        this.employeeId = employeeId;
        this.action = action;
        this.severity = severity;
        this.disposition = disposition;
        this.status = status;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getFindingCode() { return findingCode; }
    public void setFindingCode(String findingCode) { this.findingCode = findingCode; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public DriftSeverity getSeverity() { return severity; }
    public void setSeverity(DriftSeverity severity) { this.severity = severity; }

    public RemediationDisposition getDisposition() { return disposition; }
    public void setDisposition(RemediationDisposition disposition) { this.disposition = disposition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
