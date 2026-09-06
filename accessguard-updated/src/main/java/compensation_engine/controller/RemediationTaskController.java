package compensation_engine.controller;

import compensation_engine.dto.RemediationDecisionRequest;
import compensation_engine.model.RemediationTaskEntity;
import compensation_engine.service.RemediationQueueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/remediation/tasks")
@CrossOrigin
public class RemediationTaskController {

    private final RemediationQueueService queueService;

    public RemediationTaskController(RemediationQueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public List<RemediationTaskEntity> generate() {
        return queueService.generateTasks();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'AUDITOR', 'ADMIN')")
    public List<RemediationTaskEntity> recent() {
        return queueService.recentTasks();
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'AUDITOR', 'ADMIN')")
    public RemediationTaskEntity get(@PathVariable String taskId) {
        return queueService.find(taskId);
    }

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public RemediationTaskEntity approve(@PathVariable String taskId,
                                         @Valid @RequestBody RemediationDecisionRequest request) {
        return queueService.approve(taskId, request.getActor());
    }

    @PostMapping("/{taskId}/execute")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public RemediationTaskEntity execute(@PathVariable String taskId) {
        return queueService.execute(taskId);
    }
}
