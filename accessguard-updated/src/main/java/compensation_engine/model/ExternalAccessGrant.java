package compensation_engine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "external_access_grants")
public class ExternalAccessGrant {

    @Id
    @Column(name = "grant_id", nullable = false, unique = true)
    private String grantId;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String application;

    @Column(name = "access_level", nullable = false)
    private String accessLevel;

    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public ExternalAccessGrant() {}

    public ExternalAccessGrant(String grantId, String employeeId,
                               String application, String accessLevel,
                               String status) {
        this.grantId = grantId;
        this.employeeId = employeeId;
        this.application = application;
        this.accessLevel = accessLevel;
        this.status = status;
    }

    public String getGrantId() { return grantId; }
    public void setGrantId(String grantId) { this.grantId = grantId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
