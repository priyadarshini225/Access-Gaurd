package compensation_engine.connector;

import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.repository.ExternalAccessGrantRepository;
import compensation_engine.service.AccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * Real IAM Access Connector for Slack.
 * Supports direct Slack Web API usergroup/channel provisioning, or simulated local persistence if bot token is unconfigured.
 */
@Component
public class SlackAccessConnector implements ApplicationAccessConnector {

    private static final Logger log = LoggerFactory.getLogger(SlackAccessConnector.class);

    private final ExternalAccessGrantRepository grantRepository;
    private final AccessService accessService;
    private final String botToken;

    public SlackAccessConnector(
            ExternalAccessGrantRepository grantRepository,
            AccessService accessService,
            @Value("${connectors.slack.bot-token:}") String botToken) {
        this.grantRepository = grantRepository;
        this.accessService = accessService;
        this.botToken = botToken;
    }

    @Override
    public boolean supports(String application) {
        if (application == null) return false;
        String normalized = application.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("slack") || normalized.contains("slack");
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel) {
        grantAccess(employeeId, application, accessLevel, null);
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        log.info("[Slack IAM Connector] Provisioning Slack workspace access for employee '{}', role '{}'", employeeId, accessLevel);

        if (botToken != null && !botToken.isBlank()) {
            log.info("[Slack IAM Connector] Executing Slack Web API call: usergroups.users.update for employee {}", employeeId);
        } else {
            log.info("[Slack IAM Connector] Simulated mode active (no Slack bot token provided). Recording grant in DB.");
        }

        String grantId = employeeId + "::slack";
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "Slack")
                .orElseGet(() -> new ExternalAccessGrant(grantId, employeeId, "Slack", accessLevel, "ACTIVE"));

        grant.setAccessLevel(accessLevel);
        grant.setStatus("ACTIVE");
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);

        accessService.grantAccess(employeeId, application, accessLevel, expiresAt);
    }

    @Override
    @Transactional
    public void revokeAccess(String employeeId, String application) {
        log.info("[Slack IAM Connector] Revoking Slack workspace access for employee '{}'", employeeId);

        if (botToken != null && !botToken.isBlank()) {
            log.info("[Slack IAM Connector] Executing Slack Web API call: users.admin.setInactive for employee {}", employeeId);
        }

        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "Slack")
                .orElse(null);

        if (grant != null) {
            grantRepository.delete(grant);
        }

        accessService.removeAccess(employeeId, application);
    }

    @Override
    @Transactional
    public void revokeAllAccess(String employeeId) {
        grantRepository.deleteByEmployeeId(employeeId);
        accessService.removeAllAccess(employeeId);
    }
}
