package compensation_engine.connector;

import compensation_engine.service.AccessService;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DatabaseAccessConnector implements ApplicationAccessConnector {

    private final AccessService accessService;

    public DatabaseAccessConnector(AccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public boolean supports(String application) {
        return true;
    }

    @Override
    public boolean isFallback() {
        return true;
    }

    @Override
    public void grantAccess(String employeeId, String application, String accessLevel) {
        accessService.grantAccess(employeeId, application, accessLevel);
    }

    @Override
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        accessService.grantAccess(employeeId, application, accessLevel, expiresAt);
    }

    @Override
    public void revokeAccess(String employeeId, String application) {
        accessService.removeAccess(employeeId, application);
    }

    @Override
    public void revokeAllAccess(String employeeId) {
        accessService.removeAllAccess(employeeId);
    }
}
