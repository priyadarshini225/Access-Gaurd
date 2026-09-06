package compensation_engine.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class AccessGrantRequest {

    @NotBlank(message = "Application is required")
    private String application;

    @NotBlank(message = "Access level is required")
    private String accessLevel;
    private Instant expiresAt;

    public AccessGrantRequest() {}

    public AccessGrantRequest(String application, String accessLevel) {
        this.application = application;
        this.accessLevel = accessLevel;
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

    public Instant getExpiresAt() { return expiresAt; }

    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
