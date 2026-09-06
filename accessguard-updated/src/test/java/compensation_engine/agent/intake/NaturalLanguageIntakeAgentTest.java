package compensation_engine.agent.intake;

import compensation_engine.ai.OllamaService;
import compensation_engine.dto.AiPlanResponse;
import compensation_engine.dto.OnboardRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaturalLanguageIntakeAgentTest {

    @Mock
    private OllamaService ollamaService;

    @Test
    void convertsOnlineOnboardingPlanToStructuredRequest() {
        AiPlanResponse plan = plan("ONBOARD", "ONLINE", "Alice Smith", "Engineering",
                "Developer", "GitLab", "Developer");
        when(ollamaService.plan("onboard Alice")).thenReturn(plan);

        OnboardRequest request = new NaturalLanguageIntakeAgent(ollamaService)
                .parseOnboarding("onboard Alice");

        assertEquals("Alice Smith", request.getName());
        assertEquals("Engineering", request.getDepartment());
        assertEquals("GitLab", request.getAccessRequests().get(0).getApplication());
    }

    @Test
    void rejectsNonOnboardingIntent() {
        when(ollamaService.plan("offboard Alice"))
                .thenReturn(plan("OFFBOARD", "ONLINE", "Alice", "", "", "", ""));

        assertThrows(IllegalArgumentException.class,
                () -> new NaturalLanguageIntakeAgent(ollamaService)
                        .parseOnboarding("offboard Alice"));
    }

    @Test
    void rejectsUnavailableModel() {
        when(ollamaService.plan("onboard Alice"))
                .thenReturn(plan("UNKNOWN", "OFFLINE", "", "", "", "", ""));

        assertThrows(IllegalStateException.class,
                () -> new NaturalLanguageIntakeAgent(ollamaService)
                        .parseOnboarding("onboard Alice"));
    }

    private AiPlanResponse plan(String intent, String status, String name, String department,
                                String role, String application, String accessLevel) {
        AiPlanResponse response = new AiPlanResponse();
        response.setIntent(intent);
        response.setAiStatus(status);
        response.setName(name);
        response.setDepartment(department);
        response.setRole(role);
        response.setApplication(application);
        response.setAccessLevel(accessLevel);
        return response;
    }
}
