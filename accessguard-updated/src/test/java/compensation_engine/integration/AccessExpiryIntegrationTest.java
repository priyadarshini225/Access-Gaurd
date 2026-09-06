package compensation_engine.integration;

import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:expirytest;DB_CLOSE_DELAY=-1")
class AccessExpiryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ApplicationAccessRepository accessRepository;

    @BeforeEach
    void cleanDb() {
        accessRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void processRevokesExpiredLocalAccess() throws Exception {
        employeeRepository.save(new Employee("expiry-001", "Temporary User", "Engineering", "Developer", "ACTIVE"));
        ApplicationAccess access = new ApplicationAccess("expiry-001", "GitLab", "Developer", "ACTIVE");
        access.setExpiresAt(Instant.now().minusSeconds(60));
        accessRepository.save(access);

        mockMvc.perform(post("/api/access-expiry/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("expiry-001:GitLab")));

        org.junit.jupiter.api.Assertions.assertTrue(accessRepository.findAll().isEmpty());
    }
}
