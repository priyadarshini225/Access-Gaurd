package compensation_engine.service;

import compensation_engine.agent.remediation.RemediationAgent;
import compensation_engine.agent.remediation.RemediationDisposition;
import compensation_engine.agent.remediation.RemediationPlan;
import compensation_engine.agent.remediation.RemediationTask;
import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.model.RemediationTaskEntity;
import compensation_engine.repository.RemediationTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RemediationQueueService {

    private final RemediationTaskRepository taskRepository;
    private final RemediationAgent remediationAgent;
    private final AccountService accountService;
    private final ResourceService resourceService;
    private final ApplicationAccessConnectorRegistry connectorRegistry;
    private final AuditEventService auditEventService;

    public RemediationQueueService(RemediationTaskRepository taskRepository,
                                   RemediationAgent remediationAgent,
                                   AccountService accountService,
                                   ResourceService resourceService,
                                   ApplicationAccessConnectorRegistry connectorRegistry,
                                   AuditEventService auditEventService) {
        this.taskRepository = taskRepository;
        this.remediationAgent = remediationAgent;
        this.accountService = accountService;
        this.resourceService = resourceService;
        this.connectorRegistry = connectorRegistry;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public List<RemediationTaskEntity> generateTasks() {
        RemediationPlan plan = remediationAgent.plan();
        Instant now = Instant.now();
        List<RemediationTaskEntity> tasks = plan.getTasks().stream()
                .map(task -> toEntity(task, now))
                .toList();
        List<RemediationTaskEntity> savedTasks = taskRepository.saveAll(tasks);
        savedTasks.forEach(task -> auditEventService.record(
            task.getTaskId(), null, "RemediationAgent", "SYSTEM",
            "RemediationAgent", "REMEDIATION", "CREATE_TASK", "PROPOSED",
            "Remediation task generated from reconciliation drift."));
        return savedTasks;
    }

    @Transactional(readOnly = true)
    public List<RemediationTaskEntity> recentTasks() {
        return taskRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public RemediationTaskEntity find(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation task not found: " + taskId));
    }

    @Transactional
    public RemediationTaskEntity approve(String taskId, String actor) {
        RemediationTaskEntity task = find(taskId);
        String approver = required(actor);
        if (task.getDisposition() != RemediationDisposition.REQUIRES_APPROVAL) {
            throw new IllegalStateException("Task does not require approval: " + task.getDisposition());
        }
        if (!"PROPOSED".equals(task.getStatus())) {
            throw new IllegalStateException("Task is not awaiting approval: " + task.getStatus());
        }
        task.setApprovedBy(approver);
        task.setStatus("APPROVED");
        task.setUpdatedAt(Instant.now());
        RemediationTaskEntity saved = taskRepository.save(task);
        auditEventService.record(
            saved.getTaskId(), null, approver, "USER", null,
            "REMEDIATION", "APPROVE_TASK", saved.getStatus(),
            "Remediation task approved for execution.");
        return saved;
    }

    @Transactional
    public RemediationTaskEntity execute(String taskId) {
        RemediationTaskEntity task = find(taskId);
        if (!"APPROVED".equals(task.getStatus())
                && !(RemediationDisposition.AUTO_SAFE.equals(task.getDisposition())
                && "PROPOSED".equals(task.getStatus()))) {
            throw new IllegalStateException("Task is not approved for execution: " + task.getStatus());
        }
        if (!isExecutable(task.getFindingCode())) {
            throw new IllegalStateException("Task requires manual investigation: " + task.getFindingCode());
        }

        task.setStatus("EXECUTING");
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
        try {
            executeAction(task);
            task.setStatus("EXECUTED");
            task.setLastError(null);
                auditEventService.record(
                    task.getTaskId(), null, "RemediationQueueService", "SYSTEM",
                    "ExecutionAgent", "REMEDIATION", "EXECUTE_TASK", "SUCCESS",
                    "Remediation action completed.");
        } catch (RuntimeException exception) {
            task.setStatus("FAILED");
            task.setLastError(exception.getMessage());
            throw exception;
        } finally {
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
        }
        return task;
    }

    private void executeAction(RemediationTaskEntity task) {
        switch (task.getFindingCode()) {
            case "INACTIVE_ACCOUNT_ACTIVE" -> accountService.disableAccount(task.getEmployeeId());
            case "INACTIVE_APPLICATION_ACCESS" -> connectorRegistry.resolve("all")
                    .revokeAllAccess(task.getEmployeeId());
            case "INACTIVE_RESOURCE" -> resourceService.deleteResources(task.getEmployeeId());
            default -> throw new IllegalStateException(
                    "No automatic action is registered for: " + task.getFindingCode());
        }
    }

    private boolean isExecutable(String findingCode) {
        return "INACTIVE_ACCOUNT_ACTIVE".equals(findingCode)
                || "INACTIVE_APPLICATION_ACCESS".equals(findingCode)
                || "INACTIVE_RESOURCE".equals(findingCode);
    }

    private RemediationTaskEntity toEntity(RemediationTask task, Instant now) {
        RemediationTaskEntity entity = new RemediationTaskEntity();
        entity.setTaskId(task.getTaskId());
        entity.setFindingCode(task.getFindingCode());
        entity.setEmployeeId(task.getEmployeeId());
        entity.setAction(task.getAction());
        entity.setSeverity(task.getSeverity());
        entity.setDisposition(task.getDisposition());
        entity.setStatus("PROPOSED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private String required(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Actor is required.");
        }
        return actor.trim();
    }
}
