package compensation_engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @Column(name = "resource_id", nullable = false, unique = true)
    private String resourceId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String status;

    public Resource() {}

    public Resource(String resourceId, String employeeId,
                    String resourceType, String status) {
        this.resourceId = resourceId;
        this.employeeId = employeeId;
        this.resourceType = resourceType;
        this.status = status;
    }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
