package compensation_engine.controller;

import compensation_engine.agent.security.SecurityComplianceAgent;
import compensation_engine.agent.security.SecurityEvaluation;
import compensation_engine.dto.OnboardRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/security")
@CrossOrigin
public class SecurityController {

    private final SecurityComplianceAgent securityComplianceAgent;

    public SecurityController(SecurityComplianceAgent securityComplianceAgent) {
        this.securityComplianceAgent = securityComplianceAgent;
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('SECURITY', 'OPERATOR', 'ADMIN')")
    public SecurityEvaluation evaluate(@Valid @RequestBody OnboardRequest request) {
        return securityComplianceAgent.evaluate(request);
    }
}