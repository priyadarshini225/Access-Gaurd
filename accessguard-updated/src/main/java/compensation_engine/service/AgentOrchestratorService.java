package compensation_engine.service;

import compensation_engine.agent.orchestration.AgentExecutionResponse;
import compensation_engine.agent.intake.NaturalLanguageIntakeAgent;
import compensation_engine.dto.AgentMessageRequest;
import compensation_engine.dto.AgentExecutionRequest;
import compensation_engine.dto.ApprovalSubmissionRequest;
import compensation_engine.model.ApprovalRequest;
import compensation_engine.saga.SagaResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentOrchestratorService {

    private final ApprovalService approvalService;
    private final AuditEventService auditEventService;
    private final NaturalLanguageIntakeAgent intakeAgent;

    public AgentOrchestratorService(ApprovalService approvalService,
                                    AuditEventService auditEventService,
                                    NaturalLanguageIntakeAgent intakeAgent) {
        this.approvalService = approvalService;
        this.auditEventService = auditEventService;
        this.intakeAgent = intakeAgent;
    }

    @Transactional
    public AgentExecutionResponse execute(AgentExecutionRequest request) {
        if (request == null || request.getOnboardingRequest() == null) {
            throw new IllegalArgumentException("An onboarding request is required.");
        }

        ApprovalSubmissionRequest submission = new ApprovalSubmissionRequest();
        submission.setRequester(request.getRequester());
        submission.setIdempotencyKey(request.getIdempotencyKey());
        submission.setOnboardingRequest(request.getOnboardingRequest());

        ApprovalRequest approval = approvalService.submit(submission);
        AgentExecutionResponse response = baseResponse(approval);

        if ("EXECUTED".equals(approval.getStatus())) {
            response.setStatus("EXECUTED");
            response.setWorkflowId(approval.getWorkflowId());
            response.setMessage("Request was already executed; returning the original result.");
            return response;
        }

        if ("AUTO_APPROVED".equals(approval.getStatus())) {
            SagaResult result = approvalService.execute(approval.getRequestId());
            response.setStatus("EXECUTED");
            response.setWorkflowId(result.getWorkflowId());
            response.setMessage(result.getMessage());
            auditEventService.record(
                    approval.getRequestId(), result.getWorkflowId(), request.getRequester(), "USER",
                    "AgentOrchestrator", "ORCHESTRATION", "COORDINATE_REQUEST", "EXECUTED",
                    "Low-risk request was evaluated and executed automatically.");
        } else {
            response.setStatus("PENDING_APPROVAL");
            response.setMessage("Security approval is required before execution.");
            auditEventService.record(
                    approval.getRequestId(), null, request.getRequester(), "USER",
                    "AgentOrchestrator", "ORCHESTRATION", "COORDINATE_REQUEST", "PENDING_APPROVAL",
                    "Request was routed to the approval workflow.");
        }
        return response;
    }

    @Transactional
    public AgentExecutionResponse executeMessage(AgentMessageRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }

        var onboardingRequest = intakeAgent.parseOnboarding(request.getMessage());
        auditEventService.record(
                null, null, request.getRequester(), "USER", "IntakeAgent",
                "INTAKE", "PARSE_REQUEST", "ONBOARD",
                "Natural-language request converted into a structured onboarding request.");

        AgentExecutionRequest structuredRequest = new AgentExecutionRequest();
        structuredRequest.setRequester(request.getRequester());
        structuredRequest.setIdempotencyKey(request.getIdempotencyKey());
        structuredRequest.setOnboardingRequest(onboardingRequest);
        return execute(structuredRequest);
    }

    private AgentExecutionResponse baseResponse(ApprovalRequest approval) {
        AgentExecutionResponse response = new AgentExecutionResponse();
        response.setRequestId(approval.getRequestId());
        response.setRiskLevel(approval.getRiskLevel());
        response.setRiskScore(approval.getRiskScore());
        response.setApprovalRequired(!"AUTO_APPROVED".equals(approval.getStatus()));
        response.setDualApprovalRequired("PENDING_DUAL_APPROVAL".equals(approval.getStatus()));
        return response;
    }
}
