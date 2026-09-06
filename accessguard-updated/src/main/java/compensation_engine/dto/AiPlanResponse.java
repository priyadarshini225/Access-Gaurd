package compensation_engine.dto;

import java.util.List;

public class AiPlanResponse {

    private String intent;
    private String name;
    private String employeeId;
    private String department;
    private String role;
    private String application;
    private String accessLevel;
    private String message;
    private String aiModel;
    private String aiStatus;

    private List<ApplicationGrant> applications = new java.util.ArrayList<>();

    public static class ApplicationGrant {
        private String application;
        private String accessLevel;

        public ApplicationGrant() {}
        public ApplicationGrant(String application, String accessLevel) {
            this.application = application;
            this.accessLevel = accessLevel;
        }

        public String getApplication() { return application; }
        public void setApplication(String application) { this.application = application; }

        public String getAccessLevel() { return accessLevel; }
        public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    }

    public String getIntent()              { return intent; }
    public void setIntent(String v)        { this.intent = v; }

    public String getName()                { return name; }
    public void setName(String v)          { this.name = v; }

    public String getEmployeeId()          { return employeeId; }
    public void setEmployeeId(String v)    { this.employeeId = v; }

    public String getDepartment()          { return department; }
    public void setDepartment(String v)    { this.department = v; }

    public String getRole()                { return role; }
    public void setRole(String v)          { this.role = v; }

    public String getApplication()         { return application; }
    public void setApplication(String v)   { this.application = v; }

    public String getAccessLevel()         { return accessLevel; }
    public void setAccessLevel(String v)   { this.accessLevel = v; }

    public List<ApplicationGrant> getApplications() { return applications; }
    public void setApplications(List<ApplicationGrant> v) { this.applications = v != null ? v : new java.util.ArrayList<>(); }

    public String getMessage()             { return message; }
    public void setMessage(String v)       { this.message = v; }

    public String getAiModel()             { return aiModel; }
    public void setAiModel(String v)       { this.aiModel = v; }

    public String getAiStatus()            { return aiStatus; }
    public void setAiStatus(String v)      { this.aiStatus = v; }
}
