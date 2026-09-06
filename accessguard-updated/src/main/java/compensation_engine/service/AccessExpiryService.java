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
import java.util.List;

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

        for (ApplicationAccess access : accessRepository.findByExpiresAtBeforeAndStatus(now, "ACTIVE")) {
            accessRepository.delete(access);
            revoked.add(access.getEmployeeId() + ":" + access.getApplication());
        }

        for (ExternalAccessGrant grant : externalGrantRepository.findByExpiresAtBeforeAndStatus(now, "ACTIVE")) {
            connectorRegistry.resolve(grant.getApplication()).revokeAccess(
                    grant.getEmployeeId(), grant.getApplication());
            revoked.add(grant.getEmployeeId() + ":" + grant.getApplication());
        }

        return revoked;
    }
}
