package compensation_engine.controller;

import compensation_engine.dto.OffboardRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.dto.RevokeAccessRequest;
import compensation_engine.model.WorkflowExecution;
import compensation_engine.repository.ExternalAccessGrantRepository;
import compensation_engine.saga.SagaResult;
import compensation_engine.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin
public class WorkflowController {

    private final WorkflowService workflowService;
    private final EmployeeService employeeService;
    private final AccountService accountService;
    private final AccessService accessService;
    private final ResourceService resourceService;
    private final EmailService emailService;
    private final WorkflowExecutionService executionService;
    private final ExternalAccessGrantRepository externalAccessGrantRepository;

    public WorkflowController(WorkflowService workflowService,
                              EmployeeService employeeService,
                              AccountService accountService,
                              AccessService accessService,
                              ResourceService resourceService,
                              EmailService emailService,
                              WorkflowExecutionService executionService,
                              ExternalAccessGrantRepository externalAccessGrantRepository) {
        this.workflowService = workflowService;
        this.employeeService = employeeService;
        this.accountService = accountService;
        this.accessService = accessService;
        this.resourceService = resourceService;
        this.emailService = emailService;
        this.executionService = executionService;
        this.externalAccessGrantRepository = externalAccessGrantRepository;
    }

    @PostMapping("/onboard")
    public SagaResult onboard(@Valid @RequestBody OnboardRequest request) {
        return workflowService.executeOnboarding(request);
    }

    @PostMapping("/offboard")
    public SagaResult offboard(@Valid @RequestBody OffboardRequest request) {
        return workflowService.executeOffboarding(request);
    }

    @PostMapping("/revoke-access")
    public SagaResult revokeAccess(@Valid @RequestBody RevokeAccessRequest request) {
        return workflowService.executeAccessRevocation(request);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("employees", employeeService.findAll());
        state.put("accounts", accountService.findAll());
        state.put("applicationAccess", accessService.findAll());
        state.put("resources", resourceService.findAll());
        state.put("emails", emailService.findAllAsMap());
        state.put("externalAccess", externalAccessGrantRepository.findAll());

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(state);
    }

    @GetMapping("/history")
    public ResponseEntity<List<WorkflowExecution>> history() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(executionService.getRecentExecutions());
    }
}