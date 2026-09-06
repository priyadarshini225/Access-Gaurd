package compensation_engine.agent.orchestration;

import compensation_engine.agent.security.RiskLevel;

public class AgentExecutionResponse {

    private String requestId;
    private String workflowId;
    private String status;
    private String message;
    private RiskLevel riskLevel;
    private int riskScore;
    private boolean approvalRequired;
    private boolean dualApprovalRequired;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }

    public boolean isDualApprovalRequired() { return dualApprovalRequired; }
    public void setDualApprovalRequired(boolean dualApprovalRequired) { this.dualApprovalRequired = dualApprovalRequired; }
}
