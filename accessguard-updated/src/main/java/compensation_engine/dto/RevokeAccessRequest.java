package compensation_engine.dto;

public class RevokeAccessRequest {

    private String employeeId;
    private String name;
    private String application;
    private String failAt;

    public String getEmployeeId()          { return employeeId; }
    public void setEmployeeId(String v)    { this.employeeId = v; }

    public String getName()                { return name; }
    public void setName(String v)          { this.name = v; }

    public String getApplication()         { return application; }
    public void setApplication(String v)   { this.application = v; }

    public String getFailAt()              { return failAt; }
    public void setFailAt(String v)        { this.failAt = v; }
}
