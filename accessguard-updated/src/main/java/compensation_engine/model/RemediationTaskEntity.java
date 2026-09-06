package compensation_engine.model;

import compensation_engine.agent.reconciliation.DriftSeverity;
import compensation_engine.agent.remediation.RemediationDisposition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "remediation_tasks")
public class RemediationTaskEntity {

    @Id
    @Column(name = "task_id", nullable = false, unique = true)
    private String taskId;

    @Column(name = "finding_code", nullable = false)
    private String findingCode;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false, length = 1000)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriftSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationDisposition disposition;

    @Column(nullable = false)
    private String status;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
