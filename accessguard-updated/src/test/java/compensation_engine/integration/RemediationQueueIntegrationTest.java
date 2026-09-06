package compensation_engine.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.model.Account;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.model.Resource;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.AuditEventRepository;
import compensation_engine.repository.EmployeeRepository;
import compensation_engine.repository.RemediationTaskRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:remediationtest;DB_CLOSE_DELAY=-1")
class RemediationQueueIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ApplicationAccessRepository accessRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private RemediationTaskRepository taskRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    @BeforeEach
    void cleanDb() {
        taskRepository.deleteAll();
        auditEventRepository.deleteAll();
        accessRepository.deleteAll();
        resourceRepository.deleteAll();
        accountRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void criticalAccountDriftCanBeApprovedAndExecuted() throws Exception {
        employeeRepository.save(new Employee("rem-001", "Former User", "Engineering", "Developer", "INACTIVE"));
        accountRepository.save(new Account("acc-rem-001", "rem-001", "former.user", "ACTIVE"));
        accessRepository.save(new ApplicationAccess("rem-001", "GitLab", "Developer", "ACTIVE"));
        resourceRepository.save(new Resource("res-rem-001", "rem-001", "Cloud Storage", "ACTIVE"));

        String generated = mockMvc.perform(post("/api/remediation/tasks/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        JsonNode tasks = objectMapper.readTree(generated);
        String accountTaskId = findTaskId(tasks, "INACTIVE_ACCOUNT_ACTIVE");

        String decision = "{\"actor\":\"security-operator\"}";
        mockMvc.perform(post("/api/remediation/tasks/{taskId}/approve", accountTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/remediation/tasks/{taskId}/execute", accountTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/audit/requests/{requestId}", accountTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("[0].eventType").value("REMEDIATION"))
            .andExpect(jsonPath("[1].action").value("APPROVE_TASK"))
            .andExpect(jsonPath("[2].action").value("EXECUTE_TASK"));

        org.junit.jupiter.api.Assertions.assertEquals("DISABLED",
                accountRepository.findFirstByEmployeeId("rem-001").orElseThrow().getStatus());
    }

    private String findTaskId(JsonNode tasks, String findingCode) {
        for (JsonNode task : tasks) {
            if (findingCode.equals(task.get("findingCode").asText())) {
                return task.get("taskId").asText();
            }
        }
        throw new AssertionError("Task not found: " + findingCode);
    }
}
