package compensation_engine.service;

import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.ExternalAccessGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccessExpiryService {

    private final ApplicationAccessRepository accessRepository;
    private final ExternalAccessGrantRepository externalGrantRepository;
    private final ApplicationAccessConnectorRegistry connectorRegistry;

    public AccessExpiryService(ApplicationAccessRepository accessRepository,
                               ExternalAccessGrantRepository externalGrantRepository,
                               ApplicationAccessConnectorRegistry connectorRegistry) {
        this.accessRepository = accessRepository;
        this.externalGrantRepository = externalGrantRepository;
        this.connectorRegistry = connectorRegistry;
    }

    @Transactional
    public List<String> revokeExpired() {
        Instant now = Instant.now();
        List<String> revoked = new ArrayList<>();
        List<ExternalAccessGrant> expiredExternal =
                externalGrantRepository.findByExpiresAtBeforeAndStatus(now, "ACTIVE");
        Set<String> externalKeys = new HashSet<>();

        for (ExternalAccessGrant grant : expiredExternal) {
            connectorRegistry.resolve(grant.getApplication()).revokeAccess(
                    grant.getEmployeeId(), grant.getApplication());
            externalKeys.add(key(grant.getEmployeeId(), grant.getApplication()));
            revoked.add(grant.getEmployeeId() + ":" + grant.getApplication());
        }

        for (ApplicationAccess access : accessRepository.findByExpiresAtBeforeAndStatus(now, "ACTIVE")) {
            if (externalKeys.contains(key(access.getEmployeeId(), access.getApplication()))) {
                continue;
            }
            connectorRegistry.resolve(access.getApplication()).revokeAccess(
                    access.getEmployeeId(), access.getApplication());
            revoked.add(access.getEmployeeId() + ":" + access.getApplication());
        }

        return revoked;
    }

    private String key(String employeeId, String application) {
        return employeeId.toLowerCase(java.util.Locale.ROOT)
                + ":" + application.toLowerCase(java.util.Locale.ROOT);
    }
}
