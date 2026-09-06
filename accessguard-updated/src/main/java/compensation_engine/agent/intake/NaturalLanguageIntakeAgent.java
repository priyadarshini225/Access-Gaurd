package compensation_engine.agent.intake;

import compensation_engine.ai.OllamaService;
import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.dto.AiPlanResponse;
import compensation_engine.dto.OnboardRequest;
import org.springframework.stereotype.Service;

@Service
public class NaturalLanguageIntakeAgent {

    private final OllamaService ollamaService;

    public NaturalLanguageIntakeAgent(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public OnboardRequest parseOnboarding(String message) {
        AiPlanResponse plan = ollamaService.plan(message);
        if (!"ONLINE".equals(plan.getAiStatus())) {
            throw new IllegalStateException(plan.getMessage() != null
                    ? plan.getMessage() : "The intake model is unavailable.");
        }
        if (!"ONBOARD".equals(plan.getIntent())) {
            throw new IllegalArgumentException(
                    "The request is not an onboarding request: " + plan.getIntent());
        }
        require(plan.getName(), "Employee name");
        require(plan.getDepartment(), "Department");
        require(plan.getRole(), "Role");
        require(plan.getApplication(), "Application");
        require(plan.getAccessLevel(), "Access level");

        OnboardRequest request = new OnboardRequest();
        request.setName(plan.getName());
        request.setEmployeeId(plan.getEmployeeId());
        request.setDepartment(plan.getDepartment());
        request.setRole(plan.getRole());
        request.setAccessRequests(java.util.List.of(
                new AccessGrantRequest(plan.getApplication(), plan.getAccessLevel())));
        return request;
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "The intake plan is missing required field: " + field);
        }
    }
}
