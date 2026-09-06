package compensation_engine.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.dto.AiPlanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5:1.5b}")
    private String model;

    public AiPlanResponse plan(String message) {
        if (message == null || message.trim().isEmpty()) {
            return unknownResult("No request was provided. Please enter an onboarding or offboarding request.");
        }

        String userMessage = message.trim();

        String prompt = """
                You are the intent parser for an Employee Access Management System.

                Your job is ONLY to understand the user's request.
                DO NOT execute anything.
                DO NOT invent missing information.
                DO NOT assume defaults.

                Return ONLY one valid JSON object.

                Exact schema:

                {
                  "intent": "ONBOARD" or "OFFBOARD" or "UNKNOWN",
                  "name": "",
                  "employeeId": "",
                  "department": "",
                  "role": "",
                  "application": "",
                  "accessLevel": "",
                  "applications": [
                    { "application": "GitLab", "accessLevel": "Maintainer" }
                  ]
                }

                Rules:

                1. ONBOARD:
                   Use ONBOARD only when the user clearly asks to:
                   - onboard an employee
                   - create an employee account
                   - provision access
                   - give/grant access
                   - create employee access

                2. OFFBOARD:
                   Use OFFBOARD only when the user clearly asks to:
                   - offboard an employee
                   - remove employee access
                   - revoke access
                   - disable an employee account
                   - deactivate an employee

                3. UNKNOWN:
                   Use UNKNOWN when:
                   - the request is empty
                   - the request is only a greeting
                   - the request is unrelated to employee access
                   - the intention is unclear
                   - there is not enough information to identify the requested operation

                4. NEVER invent values.
                   If the user does not provide a value, return an empty string.

                6. Extract values only when they are explicitly present in the user request.

                7. employeeId: Extract only when a numeric ID (like 101, 102) is explicitly mentioned.
                   NEVER place department names (like "Security", "Engineering") or person names into employeeId.
                   If no explicit numeric ID is given, leave employeeId as "".

                8. department: The department or team (e.g. "Engineering", "Security", "Finance", "DevOps", "HR").

                User request:
                """ + userMessage;

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("format", "json");

        try {
            log.info("Calling Ollama API at {} with model {}", ollamaUrl, model);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ollamaUrl,
                    request,
                    Map.class
            );

            if (response.getBody() == null) {
                return unknownResult("Ollama returned an empty response.");
            }

            Object rawResponse = response.getBody().get("response");
            if (rawResponse == null || rawResponse.toString().trim().isEmpty()) {
                return unknownResult("Ollama did not return a valid response content.");
            }

            AiPlanResponse result = parseJson(rawResponse.toString());

            String intent = result.getIntent() != null ? result.getIntent().trim().toUpperCase() : "UNKNOWN";
            if (!"ONBOARD".equals(intent) && !"OFFBOARD".equals(intent)) {
                return unknownResult("The request could not be understood as a valid onboarding or offboarding operation.");
            }

            result.setIntent(intent);
            result.setAiModel(model);
            result.setAiStatus("ONLINE");
            return result;

        } catch (Exception e) {
            log.error("Failed to communicate with Ollama: {}", e.getMessage());
            return unknownResult("Ollama is not reachable. Please start Ollama and try again. (" + e.getMessage() + ")");
        }
    }

    public String ask(String message) {
        AiPlanResponse plan = plan(message);

        String intent = plan.getIntent();
        if ("UNKNOWN".equals(intent) || !"ONLINE".equals(plan.getAiStatus())) {
            return plan.getMessage() != null ? plan.getMessage() : "I could not understand the request.";
        }

        StringBuilder response = new StringBuilder();
        response.append("Intent: ").append(intent).append("\n");
        appendIfPresent(response, "Employee ID", plan.getEmployeeId());
        appendIfPresent(response, "Employee", plan.getName());
        appendIfPresent(response, "Department", plan.getDepartment());
        appendIfPresent(response, "Role", plan.getRole());
        appendIfPresent(response, "Application", plan.getApplication());
        appendIfPresent(response, "Access Level", plan.getAccessLevel());

        return response.toString().trim();
    }

    /**
     * Robust JSON parsing using Jackson ObjectMapper.
     */
    public AiPlanResponse parseJson(String raw) {
        try {
            String json = raw.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            JsonNode root = objectMapper.readTree(json);
            AiPlanResponse plan = new AiPlanResponse();

            plan.setIntent(getText(root, "intent", "UNKNOWN"));
            plan.setName(getText(root, "name", ""));
            plan.setEmployeeId(getText(root, "employeeId", ""));
            plan.setDepartment(getText(root, "department", ""));
            plan.setRole(getText(root, "role", ""));
            plan.setApplication(getText(root, "application", ""));
            plan.setAccessLevel(getText(root, "accessLevel", ""));

            if (root.has("applications") && root.get("applications").isArray()) {
                java.util.List<AiPlanResponse.ApplicationGrant> list = new java.util.ArrayList<>();
                for (JsonNode item : root.get("applications")) {
                    String app = getText(item, "application", "");
                    String lvl = getText(item, "accessLevel", "");
                    if (!app.isBlank()) {
                        list.add(new AiPlanResponse.ApplicationGrant(app, lvl));
                    }
                }
                plan.setApplications(list);
            }

            if (plan.getApplications().isEmpty() && !plan.getApplication().isBlank()) {
                plan.getApplications().add(new AiPlanResponse.ApplicationGrant(plan.getApplication(), plan.getAccessLevel()));
            }

            return plan;
        } catch (Exception ex) {
            log.warn("Jackson JSON parsing failed on LLM output: '{}', error: {}", raw, ex.getMessage());
            return unknownResult("Failed to parse AI response into structured plan: " + ex.getMessage());
        }
    }

    private String getText(JsonNode node, String fieldName, String defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText("").trim();
        }
        return defaultValue;
    }

    private AiPlanResponse unknownResult(String message) {
        AiPlanResponse result = new AiPlanResponse();
        result.setIntent("UNKNOWN");
        result.setMessage(message);
        result.setAiModel(model);
        result.setAiStatus("OFFLINE");
        return result;
    }

    private void appendIfPresent(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(": ").append(value).append("\n");
        }
    }
}