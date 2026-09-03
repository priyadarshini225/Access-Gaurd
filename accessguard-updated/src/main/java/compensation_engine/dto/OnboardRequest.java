package compensation_engine.dto;

import jakarta.validation.constraints.NotBlank;

public class OnboardRequest {

    private String employeeId;

    @NotBlank(message = "Employee name is required")
    private String name;

    private String department;
    private String role;
    private String application;
    private String accessLevel;

    /** Optional: injected failure point for simulation testing. */
    private String failAt;

    public String getEmployeeId()              { return employeeId; }
    public void setEmployeeId(String v)        { this.employeeId = v; }

    public String getName()                    { return name; }
    public void setName(String v)              { this.name = v; }

    public String getDepartment()              { return department; }
    public void setDepartment(String v)        { this.department = v; }

    public String getRole()                    { return role; }
    public void setRole(String v)              { this.role = v; }

    public String getApplication()             { return application; }
    public void setApplication(String v)       { this.application = v; }

    public String getAccessLevel()             { return accessLevel; }
    public void setAccessLevel(String v)       { this.accessLevel = v; }

    public String getFailAt()                  { return failAt; }
    public void setFailAt(String v)            { this.failAt = v; }
}
