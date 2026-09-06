package compensation_engine.agent.identity;

import java.util.ArrayList;
import java.util.List;

public class IdentityResolutionResult {

    public enum Status {
        EXACT_MATCH,
        AMBIGUOUS_MATCH,
        NOT_FOUND,
        NEW_IDENTITY
    }

    private Status status;
    private String resolvedEmployeeId;
    private CandidateMatch exactMatch;
    private List<CandidateMatch> candidates = new ArrayList<>();
    private String message;

    public IdentityResolutionResult() {}

    public static IdentityResolutionResult exact(CandidateMatch match) {
        IdentityResolutionResult result = new IdentityResolutionResult();
        result.setStatus(Status.EXACT_MATCH);
        result.setResolvedEmployeeId(match.getEmployeeId());
        result.setExactMatch(match);
        result.getCandidates().add(match);
        result.setMessage("Successfully resolved to employee ID " + match.getEmployeeId() + " (" + match.getName() + ").");
        return result;
    }

    public static IdentityResolutionResult ambiguous(List<CandidateMatch> candidates, String query) {
        IdentityResolutionResult result = new IdentityResolutionResult();
        result.setStatus(Status.AMBIGUOUS_MATCH);
        result.setCandidates(candidates);
        result.setMessage("Ambiguous match: found " + candidates.size() + " employees matching '" + query + "'. Please specify the employee ID.");
        return result;
    }

    public static IdentityResolutionResult notFound(String query) {
        IdentityResolutionResult result = new IdentityResolutionResult();
        result.setStatus(Status.NOT_FOUND);
        result.setMessage("No matching employee record found for '" + query + "'.");
        return result;
    }

    public static IdentityResolutionResult newIdentity(String proposedName) {
        IdentityResolutionResult result = new IdentityResolutionResult();
        result.setStatus(Status.NEW_IDENTITY);
        result.setMessage("No existing employee found for '" + proposedName + "'. Clean identity ready for onboarding.");
        return result;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getResolvedEmployeeId() { return resolvedEmployeeId; }
    public void setResolvedEmployeeId(String resolvedEmployeeId) { this.resolvedEmployeeId = resolvedEmployeeId; }

    public CandidateMatch getExactMatch() { return exactMatch; }
    public void setExactMatch(CandidateMatch exactMatch) { this.exactMatch = exactMatch; }

    public List<CandidateMatch> getCandidates() { return candidates; }
    public void setCandidates(List<CandidateMatch> candidates) { this.candidates = candidates != null ? candidates : new ArrayList<>(); }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
