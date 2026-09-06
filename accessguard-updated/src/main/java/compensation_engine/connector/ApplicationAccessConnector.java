package compensation_engine.connector;

import java.time.Instant;

public interface ApplicationAccessConnector {

    boolean supports(String application);

    default boolean isFallback() {
        return false;
    }

    void grantAccess(String employeeId, String application, String accessLevel);

    default void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        grantAccess(employeeId, application, accessLevel);
    }

    void revokeAccess(String employeeId, String application);

    void revokeAllAccess(String employeeId);
}
