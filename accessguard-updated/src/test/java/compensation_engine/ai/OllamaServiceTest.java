package compensation_engine.ai;

import compensation_engine.dto.AiPlanResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaServiceTest {

    private final OllamaService ollamaService = new OllamaService();

    @Test
    @DisplayName("Should successfully parse clean JSON from LLM")
    void testParseCleanJson() {
        String raw = """
                {
                  "intent": "ONBOARD",
                  "name": "Kavya",
                  "employeeId": "emp-42",
                  "department": "Engineering",
                  "role": "Developer",
                  "application": "GitLab",
                  "accessLevel": "Maintainer"
                }
                """;

        AiPlanResponse response = ollamaService.parseJson(raw);

        assertEquals("ONBOARD", response.getIntent());
        assertEquals("Kavya", response.getName());
        assertEquals("emp-42", response.getEmployeeId());
        assertEquals("Engineering", response.getDepartment());
        assertEquals("Developer", response.getRole());
        assertEquals("GitLab", response.getApplication());
        assertEquals("Maintainer", response.getAccessLevel());
    }

    @Test
    @DisplayName("Should parse markdown-wrapped JSON (```json ... ```)")
    void testParseMarkdownWrappedJson() {
        String raw = """
                ```json
                {
                  "intent": "OFFBOARD",
                  "name": "Bob",
                  "employeeId": "",
                  "department": "",
                  "role": "",
                  "application": "GitLab",
                  "accessLevel": ""
                }
                ```
                """;

        AiPlanResponse response = ollamaService.parseJson(raw);

        assertEquals("OFFBOARD", response.getIntent());
        assertEquals("Bob", response.getName());
        assertEquals("GitLab", response.getApplication());
    }

    @Test
    @DisplayName("Should gracefully handle malformed JSON without crashing")
    void testParseMalformedJson() {
        String raw = "Sorry, I am an AI and I cannot help with that.";

        AiPlanResponse response = ollamaService.parseJson(raw);

        assertEquals("UNKNOWN", response.getIntent());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Failed to parse"));
    }

    @Test
    @DisplayName("Should ignore unexpected additional fields safely")
    void testIgnoreAdditionalFields() {
        String raw = """
                {
                  "intent": "ONBOARD",
                  "name": "Alice",
                  "extraField": 12345,
                  "nestedObj": { "key": "val" },
                  "application": "Slack"
                }
                """;

        AiPlanResponse response = ollamaService.parseJson(raw);

        assertEquals("ONBOARD", response.getIntent());
        assertEquals("Alice", response.getName());
        assertEquals("Slack", response.getApplication());
    }
}
