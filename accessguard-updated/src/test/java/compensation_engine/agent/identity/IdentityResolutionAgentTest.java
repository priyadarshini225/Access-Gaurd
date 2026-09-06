package compensation_engine.agent.identity;

import compensation_engine.model.Email;
import compensation_engine.model.Employee;
import compensation_engine.repository.EmailRepository;
import compensation_engine.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class IdentityResolutionAgentTest {

    private EmployeeRepository employeeRepository;
    private EmailRepository emailRepository;
    private IdentityResolutionAgent agent;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        emailRepository = Mockito.mock(EmailRepository.class);
        agent = new IdentityResolutionAgent(employeeRepository, emailRepository);
    }

    @Test
    void resolveByExactEmployeeId() {
        Employee emp = new Employee("101", "Alice Smith", "Engineering", "Developer", "ACTIVE");
        when(employeeRepository.findById("101")).thenReturn(Optional.of(emp));
        when(emailRepository.findById("101")).thenReturn(Optional.of(new Email("101", "alice@company.com")));

        IdentityResolutionResult result = agent.resolve("101", null, null);

        assertEquals(IdentityResolutionResult.Status.EXACT_MATCH, result.getStatus());
        assertEquals("101", result.getResolvedEmployeeId());
        assertEquals("Alice Smith", result.getCandidates().get(0).getName());
        assertEquals("alice@company.com", result.getCandidates().get(0).getEmail());
    }

    @Test
    void resolveByCorporateEmail() {
        Employee emp = new Employee("102", "Bob Jones", "Finance", "Analyst", "ACTIVE");
        Email email = new Email("102", "bob.jones@company.com");

        when(employeeRepository.findById("102")).thenReturn(Optional.of(emp));
        when(emailRepository.findAll()).thenReturn(List.of(email));

        IdentityResolutionResult result = agent.resolve(null, null, "bob.jones@company.com");

        assertEquals(IdentityResolutionResult.Status.EXACT_MATCH, result.getStatus());
        assertEquals("102", result.getResolvedEmployeeId());
        assertEquals("Bob Jones", result.getCandidates().get(0).getName());
    }

    @Test
    void resolveByUniqueNameMatch() {
        Employee emp = new Employee("103", "Carol Danvers", "Operations", "Manager", "ACTIVE");
        when(employeeRepository.findByNameIgnoreCase("Carol Danvers")).thenReturn(List.of(emp));
        when(emailRepository.findById("103")).thenReturn(Optional.empty());

        IdentityResolutionResult result = agent.resolve(null, "Carol Danvers", null);

        assertEquals(IdentityResolutionResult.Status.EXACT_MATCH, result.getStatus());
        assertEquals("103", result.getResolvedEmployeeId());
    }

    @Test
    void resolveDetectsAmbiguityForDuplicateNames() {
        Employee emp1 = new Employee("104", "John Smith", "Engineering", "Dev", "ACTIVE");
        Employee emp2 = new Employee("105", "John Smith", "Sales", "Rep", "ACTIVE");
        when(employeeRepository.findByNameIgnoreCase("John Smith")).thenReturn(List.of(emp1, emp2));
        when(emailRepository.findById(anyString())).thenReturn(Optional.empty());

        IdentityResolutionResult result = agent.resolve(null, "John Smith", null);

        assertEquals(IdentityResolutionResult.Status.AMBIGUOUS_MATCH, result.getStatus());
        assertEquals(2, result.getCandidates().size());
    }

    @Test
    void resolveByPartialSubstringMatch() {
        Employee emp = new Employee("106", "David Miller", "Engineering", "DevOps", "ACTIVE");
        when(employeeRepository.findByNameIgnoreCase("David")).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of(emp));
        when(emailRepository.findById("106")).thenReturn(Optional.empty());

        IdentityResolutionResult result = agent.resolve(null, "David", null);

        assertEquals(IdentityResolutionResult.Status.EXACT_MATCH, result.getStatus());
        assertEquals("106", result.getResolvedEmployeeId());
    }

    @Test
    void searchCandidatesFiltersByDepartmentAndRole() {
        Employee emp1 = new Employee("107", "Eva Green", "HR", "Recruiter", "ACTIVE");
        Employee emp2 = new Employee("108", "Frank Wright", "Engineering", "Lead", "ACTIVE");
        when(employeeRepository.findAll()).thenReturn(List.of(emp1, emp2));
        when(emailRepository.findById(anyString())).thenReturn(Optional.empty());

        List<CandidateMatch> results = agent.searchCandidates("", "Engineering", "");

        assertEquals(1, results.size());
        assertEquals("Frank Wright", results.get(0).getName());
    }
}
