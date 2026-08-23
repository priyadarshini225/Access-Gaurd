package compensation_engine.controller;

import compensation_engine.saga.Saga;
import compensation_engine.saga.SagaStep;
import compensation_engine.saga.WorkflowPolicy;
import compensation_engine.tools.AccessManagementTools;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin
public class WorkflowController {

    private final AccessManagementTools tools =
            new AccessManagementTools();

    @PostMapping("/onboard")
    public Map<String, Object> onboard(
            @RequestParam String employeeId,
            @RequestParam String name,
            @RequestParam String department,
            @RequestParam String role,
            @RequestParam(defaultValue = "false") boolean fail) {

        List<Map<String, String>> steps = new ArrayList<>();

        try {

            // STEP 1
            try {
                tools.createEmployee(
                        employeeId,
                        name,
                        department,
                        role
                );

                steps.add(step(
                        "Create Employee",
                        "SUCCESS"
                ));

            } catch (Exception e) {

                steps.add(step(
                        "Create Employee",
                        "FAILED"
                ));

                throw e;
            }

            // STEP 2
            try {
                tools.createAccount(
                        employeeId,
                        name.toLowerCase()
                );

                steps.add(step(
                        "Create Account",
                        "SUCCESS"
                ));

            } catch (Exception e) {

                steps.add(step(
                        "Create Account",
                        "FAILED"
                ));

                compensateEmployee(employeeId, steps);

                return result(
                        "ROLLED_BACK",
                        steps,
                        "Account creation failed"
                );
            }

            // STEP 3
            try {

                tools.createEmail(employeeId);

                steps.add(step(
                        "Create Email",
                        "SUCCESS"
                ));

            } catch (Exception e) {

                steps.add(step(
                        "Create Email",
                        "FAILED"
                ));

                compensateEmployee(employeeId, steps);

                return result(
                        "ROLLED_BACK",
                        steps,
                        "Email creation failed"
                );
            }

            // STEP 4
            try {

                tools.grantApplicationAccess(
                        employeeId,
                        "GitLab",
                        "Developer"
                );

                steps.add(step(
                        "Grant GitLab Access",
                        "SUCCESS"
                ));

            } catch (Exception e) {

                steps.add(step(
                        "Grant GitLab Access",
                        "FAILED"
                ));

                compensateEmployee(employeeId, steps);

                return result(
                        "ROLLED_BACK",
                        steps,
                        "GitLab access failed"
                );
            }

            // STEP 5
            try {

                if (fail) {
                    throw new RuntimeException(
                            "Storage service unavailable"
                    );
                }

                tools.createResource(
                        employeeId,
                        "Cloud Storage"
                );

                steps.add(step(
                        "Create Storage",
                        "SUCCESS"
                ));

            } catch (Exception e) {

                steps.add(step(
                        "Create Storage",
                        "FAILED"
                ));

                compensateEmployee(employeeId, steps);

                return result(
                        "ROLLED_BACK",
                        steps,
                        "Storage creation failed"
                );
            }

            return result(
                    "SUCCESS",
                    steps,
                    "Employee onboarding completed"
            );

        } catch (Exception e) {

            return result(
                    "FAILED",
                    steps,
                    e.getMessage()
            );
        }
    }

    private void compensateEmployee(
            String employeeId,
            List<Map<String, String>> steps) {

        // Reverse order

        tools.removeApplicationAccess(
                employeeId,
                "GitLab"
        );

        steps.add(step(
                "Compensate GitLab Access",
                "ROLLED_BACK"
        ));

        tools.deleteEmail(employeeId);

        steps.add(step(
                "Compensate Email",
                "ROLLED_BACK"
        ));

        tools.deleteAccount(employeeId);

        steps.add(step(
                "Compensate Account",
                "ROLLED_BACK"
        ));

        tools.deleteEmployee(employeeId);

        steps.add(step(
                "Compensate Employee",
                "ROLLED_BACK"
        ));
    }

    private Map<String, String> step(
            String name,
            String status) {

        Map<String, String> map =
                new LinkedHashMap<>();

        map.put("name", name);
        map.put("status", status);

        return map;
    }

    private Map<String, Object> result(
            String status,
            List<Map<String, String>> steps,
            String message) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("status", status);
        result.put("message", message);
        result.put("steps", steps);

        return result;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {

        return Map.of(
                "employees",
                tools.getEmployees(),

                "accounts",
                tools.getAccounts(),

                "applicationAccess",
                tools.getApplicationAccess(),

                "resources",
                tools.getResources()
        );
    }
}