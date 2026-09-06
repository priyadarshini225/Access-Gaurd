package compensation_engine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ApprovalSubmissionRequest {

    @NotBlank(message = "Requester is required")
    private String requester;

    private String idempotencyKey;

    @Valid
    @NotNull(message = "Onboarding request is required")
    private OnboardRequest onboardingRequest;

    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OnboardRequest getOnboardingRequest() { return onboardingRequest; }
    public void setOnboardingRequest(OnboardRequest onboardingRequest) { this.onboardingRequest = onboardingRequest; }
}
