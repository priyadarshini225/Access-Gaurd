package compensation_engine.exception;

public class EmployeeAlreadyExistsException extends RuntimeException {

    public EmployeeAlreadyExistsException(String employeeId) {
        super("Employee already exists: " + employeeId);
    }
}
