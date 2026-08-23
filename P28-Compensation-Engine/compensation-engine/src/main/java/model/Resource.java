package compensation_engine.model;

public class Resource {

    private String resourceId;
    private String employeeId;
    private String resourceType;
    private String status;

    public Resource() {
    }

    public Resource(String resourceId,
                    String employeeId,
                    String resourceType,
                    String status) {
        this.resourceId = resourceId;
        this.employeeId = employeeId;
        this.resourceType = resourceType;
        this.status = status;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}