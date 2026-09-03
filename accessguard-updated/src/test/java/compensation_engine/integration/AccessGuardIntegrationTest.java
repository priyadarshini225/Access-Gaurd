package compensation_engine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import compensation_engine.dto.OffboardRequest;
import compensation_engine.dto.OnboardRequest;
import compensation_engine.dto.RevokeAccessRequest;
import compensation_engine.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class AccessGuardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private compensation_engine.repository.AccountRepository accountRepository;

    @Autowired
    private compensation_engine.repository.ApplicationAccessRepository applicationAccessRepository;

    @Autowired
    private compensation_engine.repository.ResourceRepository resourceRepository;

    @Autowired
    private compensation_engine.repository.EmailRepository emailRepository;

    @Autowired
    private compensation_engine.repository.WorkflowExecutionRepository workflowExecutionRepository;

    @BeforeEach
    void cleanDb() {
        applicationAccessRepository.deleteAll();
        accountRepository.deleteAll();
        resourceRepository.deleteAll();
        emailRepository.deleteAll();
        employeeRepository.deleteAll();
        workflowExecutionRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/compensation/health should return 200 OK")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/compensation/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("End-to-End: Onboard -> Verify State -> Revoke App Access -> Offboard by Name")
    void testFullHappyPathWorkflow() throws Exception {
        // 1. ONBOARD
        OnboardRequest onboard = new OnboardRequest();
        onboard.setName("Kavya Sharma");
        onboard.setEmployeeId("kavya-001");
        onboard.setDepartment("Engineering");
        onboard.setRole("Lead Architect");
        onboard.setApplication("GitLab");
        onboard.setAccessLevel("Maintainer");

        mockMvc.perform(post("/api/workflow/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps", hasSize(5)));

        // 2. VERIFY STATE
        mockMvc.perform(get("/api/workflow/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees", hasSize(1)))
                .andExpect(jsonPath("$.employees[0].employeeId").value("kavya-001"))
                .andExpect(jsonPath("$.employees[0].name").value("Kavya Sharma"))
                .andExpect(jsonPath("$.accounts", hasSize(1)))
                .andExpect(jsonPath("$.applicationAccess", hasSize(1)))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.emails['kavya-001']").value("kavyasharma@company.local"));

        // 3. REVOKE ACCESS (GitLab only - employee and account stay active)
        RevokeAccessRequest revoke = new RevokeAccessRequest();
        revoke.setEmployeeId("kavya-001");
        revoke.setApplication("GitLab");

        mockMvc.perform(post("/api/workflow/revoke-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revoke)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify application access removed, but employee is still ACTIVE
        mockMvc.perform(get("/api/workflow/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationAccess", hasSize(0)))
                .andExpect(jsonPath("$.employees[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.accounts[0].status").value("ACTIVE"));

        // 4. COMPLETE OFFBOARD BY NAME (Resolves 'Kavya Sharma' -> 'kavya-001')
        OffboardRequest offboard = new OffboardRequest();
        offboard.setName("Kavya Sharma");

        mockMvc.perform(post("/api/workflow/offboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offboard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify employee deactivated and account disabled
        mockMvc.perform(get("/api/workflow/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.accounts[0].status").value("DISABLED"));

        // 5. VERIFY HISTORY
        mockMvc.perform(get("/api/workflow/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Onboarding Failure Simulation: Should roll back created resources in reverse order")
    void testOnboardingFailureRollback() throws Exception {
        OnboardRequest req = new OnboardRequest();
        req.setName("Test Rollback");
        req.setEmployeeId("emp-rollback-01");
        req.setDepartment("QA");
        req.setRole("Tester");
        req.setApplication("Jira");
        req.setAccessLevel("User");
        req.setFailAt("Create Storage"); // 5th step fails, steps 1-4 should roll back

        mockMvc.perform(post("/api/compensation/test/onboarding-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ROLLED_BACK"))
                .andExpect(jsonPath("$.failedStep").value("Create Storage"))
                .andExpect(jsonPath("$.compensation", hasSize(4)));

        // State should be completely clean (no leftover employee or account)
        mockMvc.perform(get("/api/workflow/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees", hasSize(0)))
                .andExpect(jsonPath("$.accounts", hasSize(0)));
    }

    @Test
    @DisplayName("Offboarding Failure Simulation: Should keep partial completion and block rollback")
    void testOffboardingFailurePartialSafety() throws Exception {
        // Setup existing employee
        OnboardRequest setup = new OnboardRequest();
        setup.setName("Offboard Target");
        setup.setEmployeeId("emp-offboard-01");
        setup.setDepartment("Ops");
        setup.setRole("Admin");
        setup.setApplication("AWS");
        setup.setAccessLevel("Root");

        mockMvc.perform(post("/api/workflow/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setup)))
                .andExpect(status().isOk());

        // Fail at step 3: Revoke Resources (steps 1 & 2 are complete)
        OffboardRequest offboard = new OffboardRequest();
        offboard.setEmployeeId("emp-offboard-01");
        offboard.setApplication("AWS");
        offboard.setFailAt("Revoke Resources");

        mockMvc.perform(post("/api/compensation/test/offboarding-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offboard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.residual").value("Revoke Resources"))
                .andExpect(jsonPath("$.compensation", hasSize(0))); // No rollback attempted!

        // Verify step 1 & 2 remained done (AWS access is gone and account is disabled)
        mockMvc.perform(get("/api/workflow/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationAccess", hasSize(0)))
                .andExpect(jsonPath("$.accounts[0].status").value("DISABLED"));
    }

    @Test
    @DisplayName("Offboarding with unknown name should return 404 Not Found")
    void testOffboardNotFound() throws Exception {
        OffboardRequest offboard = new OffboardRequest();
        offboard.setName("Non Existent Person");

        mockMvc.perform(post("/api/workflow/offboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offboard)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
