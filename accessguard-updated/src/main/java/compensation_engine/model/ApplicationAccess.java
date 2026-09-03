package compensation_engine.model;

import jakarta.persistence.*;

@Entity
@Table(name = "application_access")
public class ApplicationAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String application;

    @Column(name = "access_level", nullable = false)
    private String accessLevel;

    @Column(nullable = false)
    private String status;

    public ApplicationAccess() {}

    public ApplicationAccess(String employeeId, String application,
                              String accessLevel, String status) {
        this.employeeId = employeeId;
        this.application = application;
        this.accessLevel = accessLevel;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
