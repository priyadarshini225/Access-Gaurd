package compensation_engine.controller;

import compensation_engine.saga.SagaResult;
import compensation_engine.tools.AccessManagementTools;
import compensation_engine.workflow.OnboardingWorkflow;
import compensation_engine.workflow.RevocationWorkflow;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compensation")
@CrossOrigin
public class CompensationController {

    private final AccessManagementTools tools;

    public CompensationController(
            AccessManagementTools tools) {

        this.tools = tools;
    }

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "status",
                "OK",

                "message",
                "AccessGuard compensation engine is running"
        );
    }

    /*
     * ---------------------------------------------------------
     * ONBOARDING FAILURE TEST
     * ---------------------------------------------------------
     *
     * This endpoint is ONLY for testing Saga compensation.
     *
     * The employee information comes from the request.
     *
     * Nothing is silently invented.
     */
    @PostMapping("/test/onboarding-failure")
    public SagaResult onboardingFailure(
            @RequestBody Map<String, Object> request) {

        String employeeId =
                required(request, "employeeId");

        String name =
                required(request, "name");

        String department =
                required(request, "department");

        String role =
                required(request, "role");

        String application =
                required(request, "application");

        String accessLevel =
                required(request, "accessLevel");

        String failAt =
                optional(request, "failAt");

        return new OnboardingWorkflow(tools).run(
                employeeId,
                name,
                department,
                role,
                application,
                accessLevel,
                failAt
        );
    }

    /*
     * ---------------------------------------------------------
     * OFFBOARDING FAILURE TEST
     * ---------------------------------------------------------
     */
    @PostMapping("/test/offboarding-failure")
    public SagaResult offboardingFailure(
            @RequestBody Map<String, Object> request) {

        String employeeId =
                required(request, "employeeId");

        String application =
                required(request, "application");

        String failAt =
                optional(request, "failAt");

        return new RevocationWorkflow(tools).run(
                employeeId,
                application,
                failAt
        );
    }

    private String required(
            Map<String, Object> request,
            String key) {

        String value =
                optional(request, key);

        if (value.isBlank()) {

            throw new IllegalArgumentException(
                    "Required field missing: " + key
            );
        }

        return value;
    }

    private String optional(
            Map<String, Object> request,
            String key) {

        if (request == null) {
            return "";
        }

        Object value =
                request.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }
}
