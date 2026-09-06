package compensation_engine.agent.remediation;

import compensation_engine.agent.reconciliation.DriftFinding;
import compensation_engine.agent.reconciliation.DriftSeverity;
import compensation_engine.agent.reconciliation.ReconciliationAgent;
import compensation_engine.agent.reconciliation.ReconciliationReport;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RemediationAgent {

    private final ReconciliationAgent reconciliationAgent;

    public RemediationAgent(ReconciliationAgent reconciliationAgent) {
        this.reconciliationAgent = reconciliationAgent;
    }

    public RemediationPlan plan() {
        return plan(reconciliationAgent.scan());
    }

    public RemediationPlan plan(ReconciliationReport report) {
        List<RemediationTask> tasks = report.getFindings().stream()
                .map(this::toTask)
                .toList();
        RemediationPlan plan = new RemediationPlan();
        plan.setGeneratedAt(Instant.now());
        plan.setTasks(tasks);
        return plan;
    }

    private RemediationTask toTask(DriftFinding finding) {
        return new RemediationTask(
                "RT-" + UUID.randomUUID(),
                finding.getCode(),
                finding.getEmployeeId(),
                finding.getRecommendedAction(),
                finding.getSeverity(),
                disposition(finding),
                "PROPOSED");
    }

    private RemediationDisposition disposition(DriftFinding finding) {
        if (finding.getSeverity() == DriftSeverity.CRITICAL
                || finding.getSeverity() == DriftSeverity.HIGH) {
            return RemediationDisposition.REQUIRES_APPROVAL;
        }
        if ("ACTIVE_EMPLOYEE_MISSING_RESOURCE".equals(finding.getCode())) {
            return RemediationDisposition.MANUAL_INVESTIGATION;
        }
        return RemediationDisposition.AUTO_SAFE;
    }
}
