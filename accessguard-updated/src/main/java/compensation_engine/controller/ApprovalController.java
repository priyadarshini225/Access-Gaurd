package compensation_engine.controller;

import compensation_engine.dto.ApprovalDecisionRequest;
import compensation_engine.dto.ApprovalSubmissionRequest;
import compensation_engine.model.ApprovalRequest;
import compensation_engine.saga.SagaResult;
import compensation_engine.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'AUDITOR', 'ADMIN')")
    public List<ApprovalRequest> recent() {
        return approvalService.recent();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'ADMIN')")
    public ApprovalRequest submit(@Valid @RequestBody ApprovalSubmissionRequest request) {
        return approvalService.submit(request);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'APPROVER', 'AUDITOR', 'ADMIN')")
    public ApprovalRequest get(@PathVariable String requestId) {
        return approvalService.find(requestId);
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApprovalRequest approve(@PathVariable String requestId,
                                   @Valid @RequestBody ApprovalDecisionRequest decision) {
        return approvalService.approve(requestId, decision.getApprover());
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApprovalRequest reject(@PathVariable String requestId,
                                  @Valid @RequestBody ApprovalDecisionRequest decision) {
        return approvalService.reject(requestId, decision.getApprover());
    }

    @PostMapping("/{requestId}/execute")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<SagaResult> execute(@PathVariable String requestId) {
        return ResponseEntity.ok(approvalService.execute(requestId));
    }
}
