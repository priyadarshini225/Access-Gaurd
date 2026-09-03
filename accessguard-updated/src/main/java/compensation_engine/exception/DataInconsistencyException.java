package compensation_engine.exception;

/**
 * Thrown when an offboarding step tries to remove a record that no longer
 * exists in the database.  This means a real data inconsistency was
 * detected — the record was deleted externally before the workflow ran.
 *
 * The exception carries enough context for an administrator to act on it.
 */
public class DataInconsistencyException extends RuntimeException {

    private final String recordType;   // e.g. "Email", "Account", "Resource"
    private final String employeeId;

    public DataInconsistencyException(String recordType, String employeeId) {
        super(buildMessage(recordType, employeeId));
        this.recordType = recordType;
        this.employeeId = employeeId;
    }

    public String getRecordType()  { return recordType; }
    public String getEmployeeId()  { return employeeId; }

    private static String buildMessage(String recordType, String employeeId) {
        return String.format(
            "DATA INCONSISTENCY: %s record for employee [%s] was not found " +
            "in the database. The record may have been deleted before or " +
            "during offboarding. Manual administrator review is required.",
            recordType, employeeId
        );
    }
}
