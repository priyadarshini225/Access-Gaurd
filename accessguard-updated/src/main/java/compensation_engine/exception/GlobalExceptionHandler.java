package compensation_engine.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ----------------------------------------------------------------
     * 404 — Employee not found
     * ---------------------------------------------------------------- */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EmployeeNotFoundException ex) {

        log.warn("Employee not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        404,
                        "Not Found",
                        ex.getMessage()
                ));
    }

    /* ----------------------------------------------------------------
     * 409 — Real data inconsistency detected during offboarding
     * ---------------------------------------------------------------- */
    @ExceptionHandler(DataInconsistencyException.class)
    public ResponseEntity<ErrorResponse> handleDataInconsistency(
            DataInconsistencyException ex) {

        log.error("DATA INCONSISTENCY: recordType={}, employeeId={}",
                ex.getRecordType(), ex.getEmployeeId());

        ErrorResponse body = new ErrorResponse(
                409,
                "Data Inconsistency",
                ex.getMessage()
        );
        body.setDetails(List.of(
                "Record type : " + ex.getRecordType(),
                "Employee ID : " + ex.getEmployeeId(),
                "Action      : Administrator must investigate and complete offboarding manually."
        ));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    /* ----------------------------------------------------------------
     * 409 — Ambiguous employee name
     * ---------------------------------------------------------------- */
    @ExceptionHandler(AmbiguousEmployeeNameException.class)
    public ResponseEntity<ErrorResponse> handleAmbiguous(
            AmbiguousEmployeeNameException ex) {

        log.warn("Ambiguous employee name: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage()
        );
        body.setDetails(
                ex.getMatchingIds().stream()
                        .map(id -> "employeeId: " + id)
                        .collect(Collectors.toList())
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    /* ----------------------------------------------------------------
     * 409 — Employee already exists
     * ---------------------------------------------------------------- */
    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(
            EmployeeAlreadyExistsException ex) {

        log.warn("Employee already exists: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        409,
                        "Conflict",
                        ex.getMessage()
                ));
    }

    /* ----------------------------------------------------------------
     * 503 — Ollama unavailable
     * ---------------------------------------------------------------- */
    @ExceptionHandler(OllamaUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleOllama(
            OllamaUnavailableException ex) {

        log.error("Ollama unavailable: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        503,
                        "Service Unavailable",
                        ex.getMessage()
                ));
    }

    /* ----------------------------------------------------------------
     * 400 — Bean Validation failures
     * ---------------------------------------------------------------- */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", details);

        ErrorResponse body = new ErrorResponse(
                400,
                "Bad Request",
                "Request validation failed."
        );
        body.setDetails(details);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /* ----------------------------------------------------------------
     * 400 — Illegal argument
     * ---------------------------------------------------------------- */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegal(
            IllegalArgumentException ex) {

        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        400,
                        "Bad Request",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex) {

        log.warn("Invalid state transition: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        409,
                        "Conflict",
                        ex.getMessage()
                ));
    }

    /* ----------------------------------------------------------------
     * 500 — Fallback
     * ---------------------------------------------------------------- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        log.error("Unexpected error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        500,
                        "Internal Server Error",
                        "An unexpected error occurred. Please check server logs."
                ));
    }
}
