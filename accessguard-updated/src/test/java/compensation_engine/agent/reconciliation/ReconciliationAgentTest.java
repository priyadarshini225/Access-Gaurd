package compensation_engine.agent.reconciliation;

import compensation_engine.model.Account;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.Employee;
import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.model.Resource;
import compensation_engine.repository.AccountRepository;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.EmployeeRepository;
import compensation_engine.repository.ExternalAccessGrantRepository;
import compensation_engine.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationAgentTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ApplicationAccessRepository accessRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ExternalAccessGrantRepository externalGrantRepository;

    @Test
    void detectsResidualAccessForInactiveEmployee() {
        Employee employee = new Employee("emp-inactive", "Former User", "Engineering", "Developer", "INACTIVE");
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(accountRepository.findAll()).thenReturn(List.of(
                new Account("acc-1", "emp-inactive", "former.user", "ACTIVE")));
        when(accessRepository.findAll()).thenReturn(List.of(
                new ApplicationAccess("emp-inactive", "GitLab", "Developer", "ACTIVE")));
        when(resourceRepository.findAll()).thenReturn(List.of(
                new Resource("res-1", "emp-inactive", "Cloud Storage", "ACTIVE")));
        when(externalGrantRepository.findAll()).thenReturn(List.of(
                new ExternalAccessGrant("grant-1", "emp-inactive", "Jira", "User", "ACTIVE")));

        ReconciliationReport report = agent().scan();

        assertEquals(4, report.getFindingCount());
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("INACTIVE_ACCOUNT_ACTIVE")));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("INACTIVE_APPLICATION_ACCESS")));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("INACTIVE_EXTERNAL_ACCESS")));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("INACTIVE_RESOURCE")));
    }

    @Test
    void detectsMissingProvisioningForActiveEmployee() {
        Employee employee = new Employee("emp-active", "New User", "Engineering", "Developer", "ACTIVE");
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(accountRepository.findAll()).thenReturn(List.of());
        when(accessRepository.findAll()).thenReturn(List.of());
        when(resourceRepository.findAll()).thenReturn(List.of());
        when(externalGrantRepository.findAll()).thenReturn(List.of());

        ReconciliationReport report = agent().scan();

        assertEquals(2, report.getFindingCount());
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("ACTIVE_EMPLOYEE_MISSING_ACCOUNT")));
        assertTrue(report.getFindings().stream().anyMatch(f -> f.getCode().equals("ACTIVE_EMPLOYEE_MISSING_RESOURCE")));
    }

    private ReconciliationAgent agent() {
        return new ReconciliationAgent(employeeRepository, accountRepository,
                accessRepository, resourceRepository, externalGrantRepository);
    }
}
