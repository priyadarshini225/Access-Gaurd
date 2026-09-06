package compensation_engine.connector;

import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.repository.ExternalAccessGrantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguredExternalAccessConnectorTest {

    @Mock
    private ExternalAccessGrantRepository grantRepository;

    @Test
    void onlyConfiguredApplicationsAreHandledExternally() {
        ConfiguredExternalAccessConnector connector =
                new ConfiguredExternalAccessConnector(grantRepository, "GitLab, Jira");

        assertTrue(connector.supports("gitlab"));
        assertTrue(connector.supports("JIRA"));
        assertFalse(connector.supports("AWS"));
        assertFalse(connector.isFallback());
    }

    @Test
    void grantCreatesPersistedExternalState() {
        ConfiguredExternalAccessConnector connector =
                new ConfiguredExternalAccessConnector(grantRepository, "GitLab");
        when(grantRepository.findByEmployeeIdAndApplicationIgnoreCase("emp-1", "GitLab"))
                .thenReturn(Optional.empty());

        connector.grantAccess("emp-1", "GitLab", "Developer");

        verify(grantRepository).save(argThat(grant ->
                "emp-1::gitlab".equals(grant.getGrantId())
                        && "ACTIVE".equals(grant.getStatus())
                        && "Developer".equals(grant.getAccessLevel())));
    }

    @Test
    void revokeMissingExternalStateRaisesDataInconsistency() {
        ConfiguredExternalAccessConnector connector =
                new ConfiguredExternalAccessConnector(grantRepository, "GitLab");
        when(grantRepository.findByEmployeeIdAndApplicationIgnoreCase("emp-1", "GitLab"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> connector.revokeAccess("emp-1", "GitLab"));
        verify(grantRepository, never()).delete(any(ExternalAccessGrant.class));
    }
}
