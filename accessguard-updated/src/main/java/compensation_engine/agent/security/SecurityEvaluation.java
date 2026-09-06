package compensation_engine.agent.security;

import java.util.ArrayList;
import java.util.List;

public class SecurityEvaluation {

    private RiskLevel riskLevel;
    private int riskScore;
    private boolean requiresApproval;
    private boolean requiresDualApproval;
    private String decision;
    private List<String> violations = new ArrayList<>();

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isRequiresDualApproval() {
        return requiresDualApproval;
    }

    public void setRequiresDualApproval(boolean requiresDualApproval) {
        this.requiresDualApproval = requiresDualApproval;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }
}
