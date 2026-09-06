package compensation_engine.service;

import compensation_engine.dto.AiPlanResponse;
import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.dto.OffboardRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.dto.RevokeAccessRequest;
import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.saga.SagaResult;
import compensation_engine.workflow.AccessRevocationWorkflow;
import compensation_engine.workflow.OffboardingWorkflow;
import compensation_engine.workflow.OnboardingWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final EmployeeService employeeService;
    private final AccountService accountService;
    private final EmailService emailService;
    private final ApplicationAccessConnectorRegistry accessConnectorRegistry;
    private final ResourceService resourceService;
    private final WorkflowExecutionService executionService;

    public WorkflowService(EmployeeService employeeService,
                           AccountService accountService,
                           EmailService emailService,
                           ResourceService resourceService,
                           ApplicationAccessConnectorRegistry accessConnectorRegistry,
                           WorkflowExecutionService executionService) {
        this.employeeService = employeeService;
        this.accountService = accountService;
        this.emailService = emailService;
        this.accessConnectorRegistry = accessConnectorRegistry;
        this.resourceService = resourceService;
        this.executionService = executionService;
    }

    public SagaResult executeOnboarding(OnboardRequest req) {
        long startTime = System.currentTimeMillis();

        String employeeId = req.getEmployeeId();
        // Preserve a caller-provided ID when it is valid and unused.
        if (employeeId == null || employeeId.isBlank() || employeeService.existsById(employeeId.trim())) {
            employeeId = employeeService.generateNextEmployeeId();
        } else {
            employeeId = employeeId.trim();
        }

        log.info("Executing Onboarding Workflow for employee: {} ({})", req.getName(), employeeId);

        OnboardingWorkflow workflow = new OnboardingWorkflow(
                employeeService, accountService, emailService, resourceService,
            accessConnectorRegistry);

        List<AccessGrantRequest> accessRequests = new ArrayList<>(req.getAccessRequests());
        if (accessRequests.isEmpty()) {
            accessRequests.add(new AccessGrantRequest(req.getApplication(), req.getAccessLevel()));
        }

        SagaResult result = workflow.run(
                employeeId,
                req.getName(),
                req.getDepartment(),
                req.getRole(),
            accessRequests,
                req.getFailAt()
        );

        long duration = System.currentTimeMillis() - startTime;
        executionService.recordExecution("ONBOARD", employeeId, result, duration);
        return result;
    }

    public SagaResult executeOffboarding(OffboardRequest req) {
        long startTime = System.currentTimeMillis();

        String employeeId = employeeService.resolveEmployeeId(req.getEmployeeId(), req.getName());
        OffboardingWorkflow workflow = new OffboardingWorkflow(
                employeeService, accountService, emailService, resourceService,
            accessConnectorRegistry);

        SagaResult result = workflow.run(employeeId, req.getApplication(), req.getFailAt());

        long duration = System.currentTimeMillis() - startTime;
        executionService.recordExecution("OFFBOARD", employeeId, result, duration);
        return result;
    }

    public SagaResult executeAccessRevocation(RevokeAccessRequest req) {
        long startTime = System.currentTimeMillis();

        String employeeId = employeeService.resolveEmployeeId(req.getEmployeeId(), req.getName());
        log.info("Executing Application Access Revocation Workflow for employeeId: {}, app: {}",
                employeeId, req.getApplication());

        AccessRevocationWorkflow workflow = new AccessRevocationWorkflow(accessConnectorRegistry);
        SagaResult result = workflow.run(employeeId, req.getApplication(), req.getFailAt());

        long duration = System.currentTimeMillis() - startTime;
        executionService.recordExecution("REVOKE_ACCESS", employeeId, result, duration);
        return result;
    }

    /**
     * Executes validated AI plan safely without allowing arbitrary execution.
     */
    public SagaResult executeFromAiPlan(AiPlanResponse plan) {
        if (plan == null || plan.getIntent() == null) {
            throw new IllegalArgumentException("Cannot execute empty or null AI plan.");
        }

        String intent = plan.getIntent().trim().toUpperCase();

        if ("ONBOARD".equals(intent)) {
            OnboardRequest req = new OnboardRequest();
            req.setName(plan.getName());
            req.setEmployeeId(plan.getEmployeeId());
            req.setDepartment(plan.getDepartment());
            req.setRole(plan.getRole());
            if (plan.getApplications() != null && !plan.getApplications().isEmpty()) {
                for (AiPlanResponse.ApplicationGrant grant : plan.getApplications()) {
                    if (grant.getApplication() != null && !grant.getApplication().isBlank()) {
                        req.getAccessRequests().add(new AccessGrantRequest(
                                grant.getApplication().trim(),
                                grant.getAccessLevel() != null ? grant.getAccessLevel().trim() : ""
                        ));
                    }
                }
            }

            if (req.getAccessRequests().isEmpty() && plan.getApplication() != null && !plan.getApplication().isBlank()) {
                req.setApplication(plan.getApplication());
                req.setAccessLevel(plan.getAccessLevel());
            }

            if (req.getName().isBlank() || req.getDepartment().isBlank() ||
                    req.getRole().isBlank() || (req.getApplication().isBlank() && req.getAccessRequests().isEmpty())) {
                throw new IllegalArgumentException("AI onboarding plan is missing required fields.");
            }

            return executeOnboarding(req);

        } else if ("OFFBOARD".equals(intent)) {
            OffboardRequest req = new OffboardRequest();
            req.setName(plan.getName());
            req.setEmployeeId(plan.getEmployeeId());
            req.setApplication(plan.getApplication());

            if ((req.getEmployeeId() == null || req.getEmployeeId().isBlank()) &&
                    (req.getName() == null || req.getName().isBlank())) {
                throw new IllegalArgumentException("AI offboarding plan requires employee name or ID.");
            }

            return executeOffboarding(req);
        } else {
            throw new IllegalArgumentException("Unsupported AI workflow intent: " + intent);
        }
    }

    /**
     * Generates a unique, human-readable Employee ID.
     * Format: EMP-XXXXXX  (6 uppercase hex characters from a UUID).
     * Example: EMP-A3F7C2
     *
     * Two employees with the same name will always get different IDs.
     */
    public static String generateUniqueId() {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "EMP-" + uuid.substring(0, 6);
    }
}
