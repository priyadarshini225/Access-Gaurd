package compensation_engine.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    private static final String MODEL =
            "qwen2.5:1.5b";

    public Map<String, String> plan(String message) {

        Map<String, String> result = new LinkedHashMap<>();

        // Never execute a workflow when there is no actual user request.
        if (message == null || message.trim().isEmpty()) {
            return unknownResult(
                    "No request was provided. Please enter an onboarding or offboarding request."
            );
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
                  "accessLevel": ""
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

                5. Do NOT use default values such as:
                   Engineering
                   Employee
                   GitLab
                   Developer

                6. Extract values only when they are explicitly present in the user request.

                7. employeeId is optional. Extract it only if explicitly provided.

                User request:
                """ + userMessage;

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", MODEL);
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("format", "json");

        try {

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            OLLAMA_URL,
                            request,
                            Map.class
                    );

            if (response.getBody() == null) {
                return unknownResult(
                        "Ollama returned an empty response."
                );
            }

            Object rawResponse =
                    response.getBody().get("response");

            if (rawResponse == null ||
                    rawResponse.toString().trim().isEmpty()) {

                return unknownResult(
                        "Ollama did not return a valid plan."
                );
            }

            result = parseJson(rawResponse.toString());

            /*
             * Safety validation.
             *
             * Even if the LLM returns something unexpected,
             * we do not allow the application to execute
             * an unknown intent.
             */
            String intent =
                    result.getOrDefault("intent", "UNKNOWN")
                            .trim()
                            .toUpperCase();

            if (!intent.equals("ONBOARD") &&
                    !intent.equals("OFFBOARD")) {

                return unknownResult(
                        "The request could not be understood as a valid onboarding or offboarding operation."
                );
            }

            result.put("intent", intent);

            result.put("aiModel", MODEL);
            result.put("aiStatus", "ONLINE");

            return result;

        } catch (Exception e) {

            return unknownResult(
                    "Ollama is not reachable. Please start Ollama and try again."
            );
        }
    }

    /**
     * Used by /api/ai/chat.
     */
    public String ask(String message) {

        Map<String, String> plan = plan(message);

        String intent =
                plan.getOrDefault("intent", "UNKNOWN");

        if ("UNKNOWN".equals(intent)) {
            return plan.getOrDefault(
                    "message",
                    "I could not understand the request."
            );
        }

        StringBuilder response = new StringBuilder();

        response.append("Intent: ")
                .append(intent)
                .append("\n");

        appendIfPresent(response, "Employee ID",
                plan.get("employeeId"));

        appendIfPresent(response, "Employee",
                plan.get("name"));

        appendIfPresent(response, "Department",
                plan.get("department"));

        appendIfPresent(response, "Role",
                plan.get("role"));

        appendIfPresent(response, "Application",
                plan.get("application"));

        appendIfPresent(response, "Access Level",
                plan.get("accessLevel"));

        return response.toString().trim();
    }

    /**
     * Parse the flat JSON returned by Ollama.
     *
     * We intentionally keep this dependency-free.
     */
    private Map<String, String> parseJson(String raw) {

        Map<String, String> result =
                new LinkedHashMap<>();

        String json = raw
                .trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        String[] fields = {
                "intent",
                "name",
                "employeeId",
                "department",
                "role",
                "application",
                "accessLevel"
        };

        for (String field : fields) {

            String pattern =
                    "\"" + Pattern.quote(field) +
                    "\"\\s*:\\s*\"([^\"]*)\"";

            Matcher matcher =
                    Pattern.compile(pattern)
                            .matcher(json);

            if (matcher.find()) {
                result.put(
                        field,
                        matcher.group(1).trim()
                );
            } else {
                result.put(field, "");
            }
        }

        return result;
    }

    /**
     * Create a safe UNKNOWN response.
     */
    private Map<String, String> unknownResult(
            String message) {

        Map<String, String> result =
                new LinkedHashMap<>();

        result.put("intent", "UNKNOWN");
        result.put("name", "");
        result.put("employeeId", "");
        result.put("department", "");
        result.put("role", "");
        result.put("application", "");
        result.put("accessLevel", "");

        result.put("message", message);
        result.put("aiModel", MODEL);
        result.put("aiStatus", "OFFLINE");

        return result;
    }

    private void appendIfPresent(
            StringBuilder builder,
            String label,
            String value) {

        if (value != null && !value.isBlank()) {

            builder.append(label)
                    .append(": ")
                    .append(value)
                    .append("\n");
        }
    }
}