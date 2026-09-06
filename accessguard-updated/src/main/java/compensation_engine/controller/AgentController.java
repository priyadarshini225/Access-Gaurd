package compensation_engine.controller;

import compensation_engine.agent.orchestration.AgentExecutionResponse;
import compensation_engine.dto.AgentExecutionRequest;
import compensation_engine.dto.AgentMessageRequest;
import compensation_engine.service.AgentOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin
public class AgentController {

    private final AgentOrchestratorService orchestratorService;

    public AgentController(AgentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/execute")
    public AgentExecutionResponse execute(@Valid @RequestBody AgentExecutionRequest request) {
        return orchestratorService.execute(request);
    }

    @PostMapping("/message")
    public AgentExecutionResponse executeMessage(@Valid @RequestBody AgentMessageRequest request) {
        return orchestratorService.executeMessage(request);
    }
}
