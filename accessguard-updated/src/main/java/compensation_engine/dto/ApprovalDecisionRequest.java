package compensation_engine.dto;

import jakarta.validation.constraints.NotBlank;

public class ApprovalDecisionRequest {

    @NotBlank(message = "Approver is required")
    private String approver;

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
}
