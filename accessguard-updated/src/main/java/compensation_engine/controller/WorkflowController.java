package compensation_engine.controller;

import compensation_engine.saga.SagaResult;
import compensation_engine.tools.AccessManagementTools;
import compensation_engine.workflow.OnboardingWorkflow;
import compensation_engine.workflow.RevocationWorkflow;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin
public class WorkflowController {

    private final AccessManagementTools tools;

    public WorkflowController(AccessManagementTools tools) {
        this.tools = tools;
    }

    @PostMapping("/onboard")
    public SagaResult onboard(@RequestBody Map<String, Object> req) {

        String name = required(req, "name");
        String employeeId = optional(req, "employeeId");

        if (employeeId.isBlank()) {
            employeeId = makeId(name);
        }

        String department = required(req, "department");
        String role = required(req, "role");
        String application = required(req, "application");
        String accessLevel = required(req, "accessLevel");

        String failAt = optional(req, "failAt");

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

    @PostMapping("/offboard")
    public SagaResult offboard(@RequestBody Map<String, Object> req) {

        String name = optional(req, "name");
        String employeeId = optional(req, "employeeId");
        String application = required(req, "application");

        /*
         * For offboarding, either employeeId or name must be supplied.
         */
        if (employeeId.isBlank() && !name.isBlank()) {
            employeeId = makeId(name);
        }

        if (employeeId.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee ID or employee name is required for offboarding."
            );
        }

        String failAt = optional(req, "failAt");

        return new RevocationWorkflow(tools).run(
                employeeId,
                application,
                failAt
        );
    }

    @GetMapping("/state")
    public Map<String, Object> state() {

        Map<String, Object> state = new LinkedHashMap<>();

        state.put("employees", tools.getEmployees());
        state.put("accounts", tools.getAccounts());
        state.put("applicationAccess", tools.getApplicationAccess());
        state.put("resources", tools.getResources());
        state.put("emails", tools.getEmails());

        return state;
    }

    /**
     * Required field.
     *
     * No default values are used.
     */
    private String required(
            Map<String, Object> request,
            String key) {

        String value = optional(request, key);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required field missing: " + key
            );
        }

        return value;
    }

    /**
     * Optional field.
     *
     * Returns an empty string instead of inventing a value.
     */
    private String optional(
            Map<String, Object> request,
            String key) {

        if (request == null) {
            return "";
        }

        Object value = request.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }

    /**
     * Creates a deterministic employee ID only when
     * the user did not explicitly provide one.
     */
    public static String makeId(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee name is required."
            );
        }

        String id = name
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                    "Could not create a valid employee ID from the employee name."
            );
        }

        return id;
    }
}