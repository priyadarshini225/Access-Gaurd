package compensation_engine.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessExpirySchedulerTest {

    @Mock
    private AccessExpiryService accessExpiryService;

    @Mock
    private AuditEventService auditEventService;

    @Test
    void disabledSchedulerDoesNotProcessAccess() {
        AccessExpiryScheduler scheduler =
                new AccessExpiryScheduler(accessExpiryService, auditEventService, false);

        scheduler.processExpiredAccess();

        verify(accessExpiryService, never()).revokeExpired();
        verify(auditEventService, never()).record(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enabledSchedulerProcessesAndAuditsRevocations() {
        when(accessExpiryService.revokeExpired()).thenReturn(List.of("emp-1:GitLab"));
        AccessExpiryScheduler scheduler =
                new AccessExpiryScheduler(accessExpiryService, auditEventService, true);

        scheduler.processExpiredAccess();

        verify(accessExpiryService).revokeExpired();
        verify(auditEventService).record(
                null, null, "AccessExpiryScheduler", "SYSTEM", "ExpiryAgent",
                "ACCESS_EXPIRY", "PROCESS_EXPIRED_ACCESS", "SUCCESS",
                "Revoked 1 expired access grant(s).");
    }
}
