package compensation_engine.model;

import jakarta.persistence.*;

@Entity
@Table(name = "access_policies")
public class AccessPolicy {

    @Id
    private String id;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String application;

    @Column(name = "max_access_level", nullable = false)
    private String maxAccessLevel;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "requires_dual_approval", nullable = false)
    private boolean requiresDualApproval;

    @Column(nullable = false)
    private boolean enabled = true;

    public AccessPolicy() {}

    public AccessPolicy(String id, String department, String role, String application, String maxAccessLevel, boolean requiresApproval, boolean requiresDualApproval, boolean enabled) {
        this.id = id;
        this.department = department;
        this.role = role;
        this.application = application;
        this.maxAccessLevel = maxAccessLevel;
        this.requiresApproval = requiresApproval;
        this.requiresDualApproval = requiresDualApproval;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getMaxAccessLevel() {
        return maxAccessLevel;
    }

    public void setMaxAccessLevel(String maxAccessLevel) {
        this.maxAccessLevel = maxAccessLevel;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isRequiresDualApproval() {
        return requiresDualApproval;
    }

    public void setRequiresDualApproval(boolean requiresDualApproval) {
        this.requiresDualApproval = requiresDualApproval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
