package compensation_engine.dto;

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

    public String getMessage()             { return message; }
    public void setMessage(String v)       { this.message = v; }

    public String getAiModel()             { return aiModel; }
    public void setAiModel(String v)       { this.aiModel = v; }

    public String getAiStatus()            { return aiStatus; }
    public void setAiStatus(String v)      { this.aiStatus = v; }
}
