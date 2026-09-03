package compensation_engine.exception;

import java.util.List;

public class AmbiguousEmployeeNameException extends RuntimeException {

    private final List<String> matchingIds;

    public AmbiguousEmployeeNameException(String name, List<String> matchingIds) {
        super("Multiple employees found with name '" + name +
              "'. Please provide an employeeId. Matching IDs: " + matchingIds);
        this.matchingIds = matchingIds;
    }

    public List<String> getMatchingIds() {
        return matchingIds;
    }
}
