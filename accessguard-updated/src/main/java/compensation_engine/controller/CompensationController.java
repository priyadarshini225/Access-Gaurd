package compensation_engine.controller;

import compensation_engine.dto.OffboardRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.saga.SagaResult;
import compensation_engine.service.WorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compensation")
@CrossOrigin
public class CompensationController {

    private final WorkflowService workflowService;

    public CompensationController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "OK",
                "message", "AccessGuard compensation engine is running"
        );
    }

    /*
     * ---------------------------------------------------------
     * ONBOARDING FAILURE TEST
     * ---------------------------------------------------------
     */
    @PostMapping("/test/onboarding-failure")
    public SagaResult onboardingFailure(@RequestBody Map<String, Object> request) {
        OnboardRequest req = new OnboardRequest();
        req.setEmployeeId(optional(request, "employeeId"));
        req.setName(required(request, "name"));
        req.setDepartment(optionalDefault(request, "department", "Engineering"));
        req.setRole(optionalDefault(request, "role", "Developer"));
        req.setApplication(optionalDefault(request, "application", "GitLab"));
        req.setAccessLevel(optionalDefault(request, "accessLevel", "Developer"));
        req.setFailAt(optional(request, "failAt"));

        return workflowService.executeOnboarding(req);
    }

    /*
     * ---------------------------------------------------------
     * OFFBOARDING FAILURE TEST
     * ---------------------------------------------------------
     */
    @PostMapping("/test/offboarding-failure")
    public SagaResult offboardingFailure(@RequestBody Map<String, Object> request) {
        OffboardRequest req = new OffboardRequest();
        req.setEmployeeId(optional(request, "employeeId"));
        req.setName(optional(request, "name"));
        req.setApplication(optional(request, "application"));
        req.setFailAt(optional(request, "failAt"));

        return workflowService.executeOffboarding(req);
    }

    private String required(Map<String, Object> request, String key) {
        String value = optional(request, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Required field missing: " + key);
        }
        return value;
    }

    private String optionalDefault(Map<String, Object> request, String key, String defaultValue) {
        String val = optional(request, key);
        return val.isBlank() ? defaultValue : val;
    }

    private String optional(Map<String, Object> request, String key) {
        if (request == null) {
            return "";
        }
        Object value = request.get(key);
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }
}
