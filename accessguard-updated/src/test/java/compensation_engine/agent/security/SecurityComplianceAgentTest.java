package compensation_engine.agent.security;

import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.dto.OnboardRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityComplianceAgentTest {

    private final SecurityComplianceAgent agent = new SecurityComplianceAgent();

    @Test
    void standardAccessIsLowRiskAndAllowed() {
        OnboardRequest request = request("Engineering", "Developer",
                new AccessGrantRequest("GitLab", "Developer"));

        SecurityEvaluation evaluation = agent.evaluate(request);

        assertEquals(RiskLevel.LOW, evaluation.getRiskLevel());
        assertEquals("ALLOWED", evaluation.getDecision());
        assertFalse(evaluation.isRequiresApproval());
        assertTrue(evaluation.getViolations().isEmpty());
    }

    @Test
    void privilegedAwsAccessRequiresApproval() {
        OnboardRequest request = request("Engineering", "Developer",
                new AccessGrantRequest("AWS", "Admin"));

        SecurityEvaluation evaluation = agent.evaluate(request);

        assertEquals(RiskLevel.HIGH, evaluation.getRiskLevel());
        assertEquals("PENDING_APPROVAL", evaluation.getDecision());
        assertTrue(evaluation.isRequiresApproval());
        assertFalse(evaluation.isRequiresDualApproval());
    }

    @Test
    void engineeringLeadProductionAdminAccessRequiresDualApproval() {
        OnboardRequest request = request("Engineering", "Engineering Lead",
                new AccessGrantRequest("Production Finance", "Superadmin"));

        SecurityEvaluation evaluation = agent.evaluate(request);

        assertEquals(RiskLevel.CRITICAL, evaluation.getRiskLevel());
        assertEquals("BLOCKED_PENDING_DUAL_APPROVAL", evaluation.getDecision());
        assertTrue(evaluation.isRequiresDualApproval());
        assertTrue(evaluation.getViolations().stream()
                .anyMatch(violation -> violation.contains("separation-of-duties")));
    }

    private OnboardRequest request(String department, String role, AccessGrantRequest access) {
        OnboardRequest request = new OnboardRequest();
        request.setName("Test User");
        request.setDepartment(department);
        request.setRole(role);
        request.setAccessRequests(List.of(access));
        return request;
    }
}