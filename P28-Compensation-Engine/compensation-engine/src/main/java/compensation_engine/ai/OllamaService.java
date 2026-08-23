package compensation_engine.ai;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/generate";

    public String ask(String userMessage) {

        String prompt = """
                You are the AI assistant for AccessGuard,
                an employee access management system.

                Your job is to understand the administrator's request
                and explain what workflow should be executed.

                Available workflows:
                1. ONBOARD employee
                2. OFFBOARD employee

                Available onboarding steps:
                - Create Employee
                - Create Account
                - Create Email
                - Grant GitLab Access
                - Create Storage

                Available offboarding steps:
                - Remove Application Access
                - Disable Account
                - Revoke Resources
                - Deactivate Employee

                IMPORTANT:
                You do NOT decide compensation or rollback.
                The Java Saga Compensation Engine makes those decisions.

                User request:
                """ + userMessage;

        Map<String, Object> request = new HashMap<>();

        request.put("model", "qwen2.5:1.5b");
        request.put("prompt", prompt);
        request.put("stream", false);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        OLLAMA_URL,
                        request,
                        Map.class
                );

        if (response.getBody() == null) {
            return "No response received from Ollama.";
        }

        Object result =
                response.getBody().get("response");

        return result != null
                ? result.toString()
                : "Ollama returned an empty response.";
    }
}