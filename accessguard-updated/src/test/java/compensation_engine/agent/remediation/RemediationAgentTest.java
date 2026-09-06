package compensation_engine.agent.remediation;

import compensation_engine.agent.reconciliation.DriftFinding;
import compensation_engine.agent.reconciliation.DriftSeverity;
import compensation_engine.agent.reconciliation.ReconciliationAgent;
import compensation_engine.agent.reconciliation.ReconciliationReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemediationAgentTest {

    @Test
    void mapsFindingsToApprovalAwareTasks() {
        ReconciliationReport report = new ReconciliationReport();
        report.setFindings(List.of(
                new DriftFinding("INACTIVE_APPLICATION_ACCESS", DriftSeverity.CRITICAL,
                        "emp-1", "ApplicationAccess", "residual access", "Revoke access"),
                new DriftFinding("ACTIVE_EMPLOYEE_MISSING_RESOURCE", DriftSeverity.MEDIUM,
                        "emp-2", "Resource", "missing resource", "Review provisioning"),
                new DriftFinding("LOW_RISK_REVIEW", DriftSeverity.LOW,
                        "emp-3", "Access", "review", "Review access")));

        RemediationPlan plan = new RemediationAgent(null).plan(report);

        assertEquals(3, plan.getTaskCount());
        assertEquals(RemediationDisposition.REQUIRES_APPROVAL,
                plan.getTasks().get(0).getDisposition());
        assertEquals(RemediationDisposition.MANUAL_INVESTIGATION,
                plan.getTasks().get(1).getDisposition());
        assertEquals(RemediationDisposition.AUTO_SAFE,
                plan.getTasks().get(2).getDisposition());
        assertEquals("PROPOSED", plan.getTasks().get(0).getStatus());
    }
}
