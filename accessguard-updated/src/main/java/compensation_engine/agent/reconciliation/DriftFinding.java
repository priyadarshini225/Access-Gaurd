package compensation_engine.agent.reconciliation;

public class DriftFinding {

    private String code;
    private DriftSeverity severity;
    private String employeeId;
    private String resourceType;
    private String message;
    private String recommendedAction;

    public DriftFinding() {}

    public DriftFinding(String code, DriftSeverity severity, String employeeId,
                        String resourceType, String message, String recommendedAction) {
        this.code = code;
        this.severity = severity;
        this.employeeId = employeeId;
        this.resourceType = resourceType;
        this.message = message;
        this.recommendedAction = recommendedAction;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DriftSeverity getSeverity() { return severity; }
    public void setSeverity(DriftSeverity severity) { this.severity = severity; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}
