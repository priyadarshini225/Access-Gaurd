package compensation_engine.controller;

import compensation_engine.agent.reconciliation.ReconciliationAgent;
import compensation_engine.agent.reconciliation.ReconciliationReport;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/reconciliation")
@CrossOrigin
public class ReconciliationController {

    private final ReconciliationAgent reconciliationAgent;

    public ReconciliationController(ReconciliationAgent reconciliationAgent) {
        this.reconciliationAgent = reconciliationAgent;
    }

    @GetMapping("/scan")
    @PreAuthorize("hasAnyRole('OPERATOR', 'AUDITOR', 'ADMIN')")
    public ReconciliationReport scan() {
        return reconciliationAgent.scan();
    }
}