package compensation_engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.agent.security.SecurityComplianceAgent;
import compensation_engine.agent.security.SecurityEvaluation;
import compensation_engine.dto.ApprovalSubmissionRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.model.ApprovalRequest;
import compensation_engine.repository.ApprovalRequestRepository;
import compensation_engine.saga.SagaResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepository;
    private final SecurityComplianceAgent securityAgent;
    private final WorkflowService workflowService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalRequestRepository approvalRepository,
                           SecurityComplianceAgent securityAgent,
                           WorkflowService workflowService,
                           AuditEventService auditEventService,
                           ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.securityAgent = securityAgent;
        this.workflowService = workflowService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ApprovalRequest submit(ApprovalSubmissionRequest submission) {
        if (submission == null || submission.getOnboardingRequest() == null) {
            throw new IllegalArgumentException("An onboarding request is required.");
        }

        OnboardRequest onboardingRequest = submission.getOnboardingRequest();
        String idempotencyKey = normalizeKey(submission.getIdempotencyKey());
        if (idempotencyKey != null) {
            ApprovalRequest existing = approvalRepository.findByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                if (!existing.getRequester().equalsIgnoreCase(submission.getRequester().trim())) {
                    throw new IllegalArgumentException("Idempotency key belongs to another requester.");
                }
                return existing;
            }
        }
        SecurityEvaluation evaluation = securityAgent.evaluate(onboardingRequest);
        ApprovalRequest approval = new ApprovalRequest();
        Instant now = Instant.now();

        approval.setRequestId("AR-" + UUID.randomUUID());
        approval.setRequester(submission.getRequester().trim());
        approval.setIdempotencyKey(idempotencyKey);
        approval.setOperation("ONBOARD");
        approval.setRequestPayload(toJson(onboardingRequest));
        approval.setRiskLevel(evaluation.getRiskLevel());
        approval.setRiskScore(evaluation.getRiskScore());
        approval.setPolicyDecision(evaluation.getDecision());
        approval.setViolations(evaluation.getViolations().stream().collect(Collectors.joining("\n")));
        approval.setStatus(evaluation.isRequiresApproval()
                ? (evaluation.isRequiresDualApproval() ? "PENDING_DUAL_APPROVAL" : "PENDING_APPROVAL")
                : "AUTO_APPROVED");
        approval.setCreatedAt(now);
        approval.setUpdatedAt(now);
        ApprovalRequest saved = approvalRepository.save(approval);
        auditEventService.record(
            saved.getRequestId(), null, saved.getRequester(), "USER",
            "SecurityComplianceAgent", "SECURITY_EVALUATION", "EVALUATE_REQUEST",
            saved.getPolicyDecision(), "Risk=" + saved.getRiskLevel()
                + ", score=" + saved.getRiskScore()
                + ", violations=" + saved.getViolations());
        return saved;
    }

    @Transactional
    public ApprovalRequest approve(String requestId, String approver) {
        ApprovalRequest request = find(requestId);
        String normalizedApprover = requiredActor(approver, "Approver");
        rejectSelfApproval(request, normalizedApprover);

        if ("PENDING_DUAL_APPROVAL".equals(request.getStatus())
            || "APPROVED_ONE".equals(request.getStatus())) {
            if (request.getFirstApprover() == null) {
                request.setFirstApprover(normalizedApprover);
                request.setStatus("APPROVED_ONE");
            } else if (request.getFirstApprover().equalsIgnoreCase(normalizedApprover)) {
                throw new IllegalArgumentException("Dual approval requires two different approvers.");
            } else {
                request.setSecondApprover(normalizedApprover);
                request.setStatus("APPROVED");
            }
        } else if ("PENDING_APPROVAL".equals(request.getStatus())) {
            request.setFirstApprover(normalizedApprover);
            request.setStatus("APPROVED");
        } else {
            throw new IllegalStateException("Request is not awaiting approval: " + request.getStatus());
        }

        request.setUpdatedAt(Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        auditEventService.record(
            saved.getRequestId(), null, normalizedApprover, "USER",
            null, "APPROVAL", "APPROVE_REQUEST", saved.getStatus(),
            "Approval state changed for request.");
        return saved;
    }

    @Transactional
    public ApprovalRequest reject(String requestId, String approver) {
        ApprovalRequest request = find(requestId);
        String normalizedApprover = requiredActor(approver, "Approver");
        rejectSelfApproval(request, normalizedApprover);

        if (!request.getStatus().startsWith("PENDING") && !"APPROVED_ONE".equals(request.getStatus())) {
            throw new IllegalStateException("Request cannot be rejected in state: " + request.getStatus());
        }
        request.setStatus("REJECTED");
        request.setFirstApprover(normalizedApprover);
        request.setUpdatedAt(Instant.now());
        ApprovalRequest saved = approvalRepository.save(request);
        auditEventService.record(
            saved.getRequestId(), null, normalizedApprover, "USER",
            null, "APPROVAL", "REJECT_REQUEST", saved.getStatus(),
            "Request rejected by approver.");
        return saved;
    }

    @Transactional
    public SagaResult execute(String requestId) {
        ApprovalRequest approval = find(requestId);
        if (!"AUTO_APPROVED".equals(approval.getStatus()) && !"APPROVED".equals(approval.getStatus())) {
            throw new IllegalStateException("Request must be approved before execution: " + approval.getStatus());
        }

        try {
            approval.setStatus("EXECUTING");
            approval.setUpdatedAt(Instant.now());
            approvalRepository.save(approval);

            OnboardRequest onboardingRequest = fromJson(approval.getRequestPayload());
            SagaResult result = workflowService.executeOnboarding(onboardingRequest);
            approval.setStatus("EXECUTED");
            approval.setWorkflowId(result.getWorkflowId());
            approval.setUpdatedAt(Instant.now());
            approvalRepository.save(approval);
                auditEventService.record(
                    approval.getRequestId(), result.getWorkflowId(), approval.getRequester(), "SYSTEM",
                    "ExecutionAgent", "WORKFLOW_EXECUTION", "EXECUTE_APPROVED_REQUEST", "SUCCESS",
                    "Approved onboarding workflow completed.");
            return result;
        } catch (RuntimeException exception) {
            approval.setStatus("EXECUTION_FAILED");
            approval.setUpdatedAt(Instant.now());
            approvalRepository.save(approval);
                auditEventService.record(
                    approval.getRequestId(), null, approval.getRequester(), "SYSTEM",
                    "ExecutionAgent", "WORKFLOW_EXECUTION", "EXECUTE_APPROVED_REQUEST", "FAILED",
                    exception.getMessage());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ApprovalRequest find(String requestId) {
        return approvalRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));
    }

    private void rejectSelfApproval(ApprovalRequest request, String approver) {
        if (request.getRequester().equalsIgnoreCase(approver)) {
            throw new IllegalArgumentException("The requester cannot approve their own request.");
        }
    }

    private String requiredActor(String actor, String fieldName) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return actor.trim();
    }

    private String normalizeKey(String key) {
        return key == null || key.isBlank() ? null : key.trim();
    }

    private String toJson(OnboardRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist the onboarding request.", exception);
        }
    }

    private OnboardRequest fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, OnboardRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not restore the approved onboarding request.", exception);
        }
    }
}
