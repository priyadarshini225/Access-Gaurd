package compensation_engine.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.enabled=true",
        "security.users.admin.password=admin-test",
        "security.users.operator.password=operator-test",
        "security.users.auditor.password=auditor-test",
        "spring.datasource.url=jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/compensation/health"))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/reset-database"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditorCanReadAuditButCannotResetDatabase() throws Exception {
        mockMvc.perform(get("/api/audit/events")
                        .with(httpBasic("auditor", "auditor-test")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/reset-database")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic("auditor", "auditor-test")))
                .andExpect(status().isForbidden());
    }
}
