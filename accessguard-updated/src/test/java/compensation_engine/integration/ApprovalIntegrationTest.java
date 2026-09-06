package compensation_engine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.dto.AccessGrantRequest;
import compensation_engine.dto.ApprovalDecisionRequest;
import compensation_engine.dto.ApprovalSubmissionRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.AuditEventRepository;
import compensation_engine.repository.ApprovalRequestRepository;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.EmailRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:approvaltest;DB_CLOSE_DELAY=-1")
class ApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApprovalRequestRepository approvalRepository;

        @Autowired
        private AuditEventRepository auditEventRepository;

    @Autowired
    private ApplicationAccessRepository accessRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanDb() {
        approvalRepository.deleteAll();
        auditEventRepository.deleteAll();
        accessRepository.deleteAll();
        accountRepository.deleteAll();
        resourceRepository.deleteAll();
        emailRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void lowRiskRequestCanBeExecutedWithoutManualApproval() throws Exception {
        ApprovalSubmissionRequest submission = submission("hr-user", "approval-low-01",
                "Engineering", "Developer", "GitLab", "Developer");

        String response = mockMvc.perform(post("/api/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTO_APPROVED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(response).get("requestId").asText();

        mockMvc.perform(post("/api/approvals/{requestId}/execute", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/audit/requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("[0].eventType").value("SECURITY_EVALUATION"))
                .andExpect(jsonPath("[1].eventType").value("WORKFLOW_EXECUTION"));
    }

    @Test
    void criticalRequestRequiresTwoDifferentApprovers() throws Exception {
        ApprovalSubmissionRequest submission = submission("engineering-lead", "approval-high-01",
                "Engineering", "Engineering Lead", "Production Finance", "Superadmin");

        String response = mockMvc.perform(post("/api/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_DUAL_APPROVAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(response).get("requestId").asText();
        ApprovalDecisionRequest first = decision("security-reviewer");
        ApprovalDecisionRequest second = decision("application-owner");

        mockMvc.perform(post("/api/approvals/{requestId}/approve", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED_ONE"));

        mockMvc.perform(post("/api/approvals/{requestId}/approve", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/audit/requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("[1].eventType").value("APPROVAL"))
                .andExpect(jsonPath("[2].eventType").value("APPROVAL"));
    }

    @Test
    void failedSagaIsNotRecordedAsExecuted() throws Exception {
        ApprovalSubmissionRequest submission = submission("hr-user", "approval-failure-01",
                "Engineering", "Developer", "GitLab", "Developer");
        submission.getOnboardingRequest().setFailAt("Create Storage");

        String response = mockMvc.perform(post("/api/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTO_APPROVED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(response).get("requestId").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/approvals/{requestId}/execute", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROLLED_BACK"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/approvals/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTION_FAILED"));
    }

    private ApprovalSubmissionRequest submission(String requester,
                                                 String employeeId,
                                                 String department,
                                                 String role,
                                                 String application,
                                                 String accessLevel) {
        OnboardRequest onboarding = new OnboardRequest();
        onboarding.setName("Approval User");
        onboarding.setEmployeeId(employeeId);
        onboarding.setDepartment(department);
        onboarding.setRole(role);
        onboarding.setAccessRequests(List.of(new AccessGrantRequest(application, accessLevel)));

        ApprovalSubmissionRequest submission = new ApprovalSubmissionRequest();
        submission.setRequester(requester);
        submission.setOnboardingRequest(onboarding);
        return submission;
    }

    private ApprovalDecisionRequest decision(String approver) {
        ApprovalDecisionRequest decision = new ApprovalDecisionRequest();
        decision.setApprover(approver);
        return decision;
    }
}
