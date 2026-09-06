package compensation_engine.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentMessageRequest {

    @NotBlank(message = "Requester is required")
    private String requester;

    private String idempotencyKey;

    @NotBlank(message = "Message is required")
    private String message;

    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
