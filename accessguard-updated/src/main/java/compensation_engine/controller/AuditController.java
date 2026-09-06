package compensation_engine.controller;

import compensation_engine.model.AuditEvent;
import compensation_engine.service.AuditEventService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin
@PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
public class AuditController {

    private final AuditEventService auditEventService;

    public AuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/events")
    public List<AuditEvent> recent() {
        return auditEventService.recent();
    }

    @GetMapping("/requests/{requestId}")
    public List<AuditEvent> forRequest(@PathVariable String requestId) {
        return auditEventService.forRequest(requestId);
    }
}
