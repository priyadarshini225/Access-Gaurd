package compensation_engine.agent.identity;

import compensation_engine.model.Email;
import compensation_engine.model.Employee;
import compensation_engine.repository.EmailRepository;
import compensation_engine.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Identity Resolution Agent
 *
 * Disambiguates and resolves employee identities across multiple input criteria
 * (ID, Full Name, Partial Name, Corporate Email, Department, Role).
 *
 * Handles exact matching, multi-candidate ambiguity detection, and clean identity
 * validation for onboarding/offboarding workflows.
 */
@Service
public class IdentityResolutionAgent {

    private static final Logger log = LoggerFactory.getLogger(IdentityResolutionAgent.class);

    private final EmployeeRepository employeeRepository;
    private final EmailRepository emailRepository;

    public IdentityResolutionAgent(EmployeeRepository employeeRepository,
                                    EmailRepository emailRepository) {
        this.employeeRepository = employeeRepository;
        this.emailRepository = emailRepository;
    }

    /**
     * Resolves an employee identity based on employeeId, name, or email.
     */
    @Transactional(readOnly = true)
    public IdentityResolutionResult resolve(String employeeId, String name, String emailQuery) {
        String trimmedId    = employeeId != null ? employeeId.trim() : "";
        String trimmedName  = name != null ? name.trim() : "";
        String trimmedEmail = emailQuery != null ? emailQuery.trim() : "";

        log.info("IdentityResolutionAgent resolving: id='{}', name='{}', email='{}'", trimmedId, trimmedName, trimmedEmail);

        // 1. Direct Employee ID lookup
        if (!trimmedId.isEmpty()) {
            Optional<Employee> byId = employeeRepository.findById(trimmedId);
            if (byId.isPresent()) {
                log.info("IdentityResolutionAgent matched exact Employee ID: {}", trimmedId);
                return IdentityResolutionResult.exact(toCandidate(byId.get()));
            }
        }

        // 2. Email Address lookup
        if (!trimmedEmail.isEmpty()) {
            Optional<Email> emailMatch = emailRepository.findAll().stream()
                    .filter(e -> e.getEmailAddress() != null && e.getEmailAddress().equalsIgnoreCase(trimmedEmail))
                    .findFirst();
            if (emailMatch.isPresent()) {
                Optional<Employee> emp = employeeRepository.findById(emailMatch.get().getEmployeeId());
                if (emp.isPresent()) {
                    log.info("IdentityResolutionAgent matched Corporate Email '{}' -> Employee ID {}", trimmedEmail, emp.get().getEmployeeId());
                    return IdentityResolutionResult.exact(toCandidate(emp.get()));
                }
            }
        }

        // 3. Exact Name Match
        String searchName = !trimmedName.isEmpty() ? trimmedName : trimmedId;
        if (!searchName.isEmpty()) {
            List<Employee> exactNameMatches = employeeRepository.findByNameIgnoreCase(searchName);
            if (exactNameMatches.size() == 1) {
                log.info("IdentityResolutionAgent matched unique name '{}' -> Employee ID {}", searchName, exactNameMatches.get(0).getEmployeeId());
                return IdentityResolutionResult.exact(toCandidate(exactNameMatches.get(0)));
            } else if (exactNameMatches.size() > 1) {
                log.warn("IdentityResolutionAgent detected ambiguity for name '{}': {} candidate matches.", searchName, exactNameMatches.size());
                List<CandidateMatch> candidates = exactNameMatches.stream().map(this::toCandidate).collect(Collectors.toList());
                return IdentityResolutionResult.ambiguous(candidates, searchName);
            }

            // 4. Fallback: check if searchName was entered as an employee ID string
            Optional<Employee> byNameAsId = employeeRepository.findById(searchName);
            if (byNameAsId.isPresent()) {
                log.info("IdentityResolutionAgent matched search target as Employee ID: {}", searchName);
                return IdentityResolutionResult.exact(toCandidate(byNameAsId.get()));
            }

            // 5. Partial / Case-Insensitive Substring Match
            List<Employee> partialMatches = employeeRepository.findAll().stream()
                    .filter(e -> e.getName() != null && e.getName().toLowerCase().contains(searchName.toLowerCase()))
                    .collect(Collectors.toList());

            if (partialMatches.size() == 1) {
                log.info("IdentityResolutionAgent matched partial substring '{}' -> Employee ID {}", searchName, partialMatches.get(0).getEmployeeId());
                return IdentityResolutionResult.exact(toCandidate(partialMatches.get(0)));
            } else if (partialMatches.size() > 1) {
                log.warn("IdentityResolutionAgent detected partial substring ambiguity for '{}': {} candidate matches.", searchName, partialMatches.size());
                List<CandidateMatch> candidates = partialMatches.stream().map(this::toCandidate).collect(Collectors.toList());
                return IdentityResolutionResult.ambiguous(candidates, searchName);
            }
        }

        log.info("IdentityResolutionAgent found no existing record for query: '{}'", searchName);
        return IdentityResolutionResult.notFound(searchName.isEmpty() ? "Unspecified search target" : searchName);
    }

    /**
     * Search employee candidates by department/role/name query.
     */
    @Transactional(readOnly = true)
    public List<CandidateMatch> searchCandidates(String query, String department, String role) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String dept = department != null ? department.trim().toLowerCase() : "";
        String r = role != null ? role.trim().toLowerCase() : "";

        return employeeRepository.findAll().stream()
                .filter(e -> q.isEmpty() || (e.getName() != null && e.getName().toLowerCase().contains(q)) || (e.getEmployeeId() != null && e.getEmployeeId().toLowerCase().contains(q)))
                .filter(e -> dept.isEmpty() || (e.getDepartment() != null && e.getDepartment().toLowerCase().contains(dept)))
                .filter(e -> r.isEmpty() || (e.getRole() != null && e.getRole().toLowerCase().contains(r)))
                .map(this::toCandidate)
                .collect(Collectors.toList());
    }

    private CandidateMatch toCandidate(Employee emp) {
        String emailAddr = emailRepository.findById(emp.getEmployeeId())
                .map(Email::getEmailAddress)
                .orElse("-");
        return new CandidateMatch(
                emp.getEmployeeId(),
                emp.getName(),
                emp.getDepartment(),
                emp.getRole(),
                emailAddr,
                emp.getStatus()
        );
    }
}
