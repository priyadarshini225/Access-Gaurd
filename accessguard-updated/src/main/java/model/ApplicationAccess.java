package compensation_engine.model;

public class ApplicationAccess {

    private String employeeId;
    private String application;
    private String accessLevel;
    private String status;

    public ApplicationAccess() {
    }

    public ApplicationAccess(String employeeId,
                             String application,
                             String accessLevel,
                             String status) {
        this.employeeId = employeeId;
        this.application = application;
        this.accessLevel = accessLevel;
        this.status = status;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}