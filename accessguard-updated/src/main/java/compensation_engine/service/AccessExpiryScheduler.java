package compensation_engine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccessExpiryScheduler.class);

    private final AccessExpiryService accessExpiryService;
    private final AuditEventService auditEventService;
    private final boolean enabled;

    public AccessExpiryScheduler(
            AccessExpiryService accessExpiryService,
            AuditEventService auditEventService,
            @Value("${access.expiry.scheduler.enabled:false}") boolean enabled) {
        this.accessExpiryService = accessExpiryService;
        this.auditEventService = auditEventService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${access.expiry.scheduler.delay-ms:300000}")
    public void processExpiredAccess() {
        if (!enabled) {
            return;
        }

        try {
            List<String> revoked = accessExpiryService.revokeExpired();
            auditEventService.record(
                    null, null, "AccessExpiryScheduler", "SYSTEM",
                    "ExpiryAgent", "ACCESS_EXPIRY", "PROCESS_EXPIRED_ACCESS", "SUCCESS",
                    "Revoked " + revoked.size() + " expired access grant(s).");
            log.info("Scheduled access expiry processing revoked {} grant(s)", revoked.size());
        } catch (RuntimeException exception) {
            auditEventService.record(
                    null, null, "AccessExpiryScheduler", "SYSTEM",
                    "ExpiryAgent", "ACCESS_EXPIRY", "PROCESS_EXPIRED_ACCESS", "FAILED",
                    exception.getMessage());
            log.error("Scheduled access expiry processing failed", exception);
        }
    }
}
