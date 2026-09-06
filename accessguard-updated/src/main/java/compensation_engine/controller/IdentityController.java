package compensation_engine.controller;

import compensation_engine.agent.identity.CandidateMatch;
import compensation_engine.agent.identity.IdentityResolutionAgent;
import compensation_engine.agent.identity.IdentityResolutionResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityResolutionAgent identityResolutionAgent;

    public IdentityController(IdentityResolutionAgent identityResolutionAgent) {
        this.identityResolutionAgent = identityResolutionAgent;
    }

    public static class ResolveRequest {
        private String employeeId;
        private String name;
        private String email;

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @PostMapping("/resolve")
    public IdentityResolutionResult resolveIdentity(@RequestBody ResolveRequest request) {
        return identityResolutionAgent.resolve(
                request.getEmployeeId(),
                request.getName(),
                request.getEmail()
        );
    }

    @GetMapping("/search")
    public List<CandidateMatch> searchCandidates(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role) {
        return identityResolutionAgent.searchCandidates(query, department, role);
    }
}
