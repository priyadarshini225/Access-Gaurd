package compensation_engine.controller;

import compensation_engine.ai.OllamaService;
import compensation_engine.dto.AiExecuteRequest;
import compensation_engine.dto.AiPlanRequest;
import compensation_engine.dto.AiPlanResponse;
import compensation_engine.saga.SagaResult;
import compensation_engine.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    private final OllamaService ai;
    private final WorkflowService workflowService;

    public AiController(OllamaService ai, WorkflowService workflowService) {
        this.ai = ai;
        this.workflowService = workflowService;
    }

    /**
     * Preview AI intent plan without executing anything.
     */
    @PostMapping("/plan")
    public AiPlanResponse plan(@Valid @RequestBody AiPlanRequest request) {
        return ai.plan(request.getMessage());
    }

    /**
     * AI chat summary response.
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@Valid @RequestBody AiPlanRequest request) {
        return Map.of("response", ai.ask(request.getMessage()));
    }

    /**
     * End-to-end flow: Natural Language -> Ollama parsing -> Application validation -> Workflow execution.
     * Guaranteed safety: only allowed predefined workflows are executed.
     */
    @PostMapping("/execute")
    public SagaResult execute(@Valid @RequestBody AiExecuteRequest request) {
        AiPlanResponse plan = ai.plan(request.getMessage());

        if (!"ONLINE".equals(plan.getAiStatus())) {
            throw new IllegalStateException(
                    plan.getMessage() != null ? plan.getMessage() : "AI service is currently unavailable."
            );
        }

        if ("UNKNOWN".equals(plan.getIntent())) {
            throw new IllegalArgumentException(
                    plan.getMessage() != null ? plan.getMessage() : "Could not understand the access request."
            );
        }

        return workflowService.executeFromAiPlan(plan);
    }
}