package compensation_engine.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.ApprovalRequestRepository;
import compensation_engine.repository.AuditEventRepository;
import compensation_engine.repository.EmployeeRepository;
import compensation_engine.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:orchestratortest;DB_CLOSE_DELAY=-1")
class AgentOrchestratorIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ApprovalRequestRepository approvalRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private ApplicationAccessRepository accessRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanDb() {
        auditEventRepository.deleteAll();
        approvalRepository.deleteAll();
        accessRepository.deleteAll();
        resourceRepository.deleteAll();
        accountRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void lowRiskRequestIsCoordinatedAndExecuted() throws Exception {
        String body = """
                {
                  "requester": "hr-user",
                  "onboardingRequest": {
                    "name": "Orchestrated User",
                    "employeeId": "orch-001",
                    "department": "Engineering",
                    "role": "Developer",
                    "accessRequests": [
                      {"application": "GitLab", "accessLevel": "Developer"}
                    ]
                  }
                }
                """;

        String response = mockMvc.perform(post("/api/agent/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.approvalRequired").value(false))
                .andExpect(jsonPath("$.riskLevel").value("LOW"))
                .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(response);
        String requestId = result.get("requestId").asText();

        mockMvc.perform(get("/api/audit/requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("[2].action").value("COORDINATE_REQUEST"));
    }

    @Test
    void highRiskRequestIsRoutedToApproval() throws Exception {
        String body = """
                {
                  "requester": "engineering-lead",
                  "onboardingRequest": {
                    "name": "Privileged User",
                    "employeeId": "orch-002",
                    "department": "Engineering",
                    "role": "Engineering Lead",
                    "accessRequests": [
                      {"application": "Production Finance", "accessLevel": "Superadmin"}
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/api/agent/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.approvalRequired").value(true))
                .andExpect(jsonPath("$.dualApprovalRequired").value(true))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));

        org.junit.jupiter.api.Assertions.assertEquals(0, employeeRepository.count());
    }

          @Test
          void repeatedIdempotencyKeyReturnsOriginalExecution() throws Exception {
        String body = """
          {
            "requester": "hr-user",
            "idempotencyKey": "onboard-orch-003-v1",
            "onboardingRequest": {
              "name": "Idempotent User",
              "employeeId": "orch-003",
              "department": "Engineering",
              "role": "Developer",
              "accessRequests": [
                {"application": "GitLab", "accessLevel": "Developer"}
              ]
            }
          }
          """;

        String first = mockMvc.perform(post("/api/agent/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("EXECUTED"))
          .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/agent/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("EXECUTED"))
          .andExpect(jsonPath("$.message").value(
            "Request was already executed; returning the original result."))
          .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(
          objectMapper.readTree(first).get("requestId").asText(),
          objectMapper.readTree(second).get("requestId").asText());
        org.junit.jupiter.api.Assertions.assertEquals(1, employeeRepository.count());
          }
}
