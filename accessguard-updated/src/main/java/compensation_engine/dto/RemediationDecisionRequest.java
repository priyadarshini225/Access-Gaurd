package compensation_engine.dto;

import jakarta.validation.constraints.NotBlank;

public class RemediationDecisionRequest {

    @NotBlank(message = "Actor is required")
    private String actor;

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
}
