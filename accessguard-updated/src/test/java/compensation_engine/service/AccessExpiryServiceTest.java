package compensation_engine.service;

import compensation_engine.connector.ApplicationAccessConnector;
import compensation_engine.connector.ApplicationAccessConnectorRegistry;
import compensation_engine.model.ApplicationAccess;
import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.repository.ApplicationAccessRepository;
import compensation_engine.repository.ExternalAccessGrantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessExpiryServiceTest {

    @Mock
    private ApplicationAccessRepository accessRepository;

    @Mock
    private ExternalAccessGrantRepository externalGrantRepository;

    @Mock
    private ApplicationAccessConnectorRegistry connectorRegistry;

    @Mock
    private ApplicationAccessConnector connector;

    @Test
    void externalGrantOwnsDuplicateLocalGrantDuringExpiry() {
        ApplicationAccess local = new ApplicationAccess("emp-1", "GitLab", "Developer", "ACTIVE");
        ExternalAccessGrant external = new ExternalAccessGrant(
                "grant-1", "emp-1", "GitLab", "Developer", "ACTIVE");
        when(accessRepository.findByExpiresAtBeforeAndStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(local));
        when(externalGrantRepository.findByExpiresAtBeforeAndStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("ACTIVE")))
                .thenReturn(List.of(external));
        when(connectorRegistry.resolve("GitLab")).thenReturn(connector);

        List<String> revoked = new AccessExpiryService(
                accessRepository, externalGrantRepository, connectorRegistry).revokeExpired();

        org.junit.jupiter.api.Assertions.assertEquals(List.of("emp-1:GitLab"), revoked);
        verify(connector, times(1)).revokeAccess("emp-1", "GitLab");
    }
}
