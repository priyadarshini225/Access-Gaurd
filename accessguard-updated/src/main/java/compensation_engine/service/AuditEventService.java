package compensation_engine.service;

import compensation_engine.model.AuditEvent;
import compensation_engine.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository auditRepository;

    public AuditEventService(AuditEventRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public AuditEvent record(String requestId,
                             String workflowId,
                             String actor,
                             String actorType,
                             String agentName,
                             String eventType,
                             String action,
                             String decision,
                             String details) {
        AuditEvent event = new AuditEvent();
        event.setEventId("AE-" + UUID.randomUUID());
        event.setRequestId(requestId);
        event.setWorkflowId(workflowId);
        event.setActor(actor);
        event.setActorType(actorType);
        event.setAgentName(agentName);
        event.setEventType(eventType);
        event.setAction(action);
        event.setDecision(decision);
        event.setDetails(details);
        event.setOccurredAt(Instant.now());
        return auditRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recent() {
        return auditRepository.findTop100ByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> forRequest(String requestId) {
        return auditRepository.findByRequestIdOrderByOccurredAtAsc(requestId);
    }
}
