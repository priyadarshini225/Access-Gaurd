package compensation_engine.exception;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String timestamp;

    /* Optional: list of field-level validation errors */
    private List<String> details;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
