package compensation_engine.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sod_rules")
public class SodRule {

    @Id
    private String id;

    @Column(nullable = false)
    private String app1;

    @Column(nullable = false)
    private String role1;

    @Column(nullable = false)
    private String app2;

    @Column(nullable = false)
    private String role2;

    @Column(nullable = false)
    private String description;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(nullable = false)
    private boolean enabled = true;

    public SodRule() {}

    public SodRule(String id, String app1, String role1, String app2, String role2, String description, String riskLevel, boolean enabled) {
        this.id = id;
        this.app1 = app1;
        this.role1 = role1;
        this.app2 = app2;
        this.role2 = role2;
        this.description = description;
        this.riskLevel = riskLevel;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApp1() {
        return app1;
    }

    public void setApp1(String app1) {
        this.app1 = app1;
    }

    public String getRole1() {
        return role1;
    }

    public void setRole1(String role1) {
        this.role1 = role1;
    }

    public String getApp2() {
        return app2;
    }

    public void setApp2(String app2) {
        this.app2 = app2;
    }

    public String getRole2() {
        return role2;
    }

    public void setRole2(String role2) {
        this.role2 = role2;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
