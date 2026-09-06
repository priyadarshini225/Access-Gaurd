package compensation_engine.controller;

import compensation_engine.agent.remediation.RemediationAgent;
import compensation_engine.agent.remediation.RemediationPlan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remediation")
@CrossOrigin
public class RemediationController {

    private final RemediationAgent remediationAgent;

    public RemediationController(RemediationAgent remediationAgent) {
        this.remediationAgent = remediationAgent;
    }

    @GetMapping("/plan")
    public RemediationPlan plan() {
        return remediationAgent.plan();
    }
}
