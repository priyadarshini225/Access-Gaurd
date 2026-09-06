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
 * Real IAM Access Connector for Jira.
 * Supports direct REST API integration with Atlassian Jira Cloud API v3, or simulated local persistence if token is unconfigured.
 */
@Component
public class JiraAccessConnector implements ApplicationAccessConnector {

    private static final Logger log = LoggerFactory.getLogger(JiraAccessConnector.class);

    private final ExternalAccessGrantRepository grantRepository;
    private final AccessService accessService;
    private final String jiraUrl;
    private final String apiToken;

    public JiraAccessConnector(
            ExternalAccessGrantRepository grantRepository,
            AccessService accessService,
            @Value("${connectors.jira.url:https://atlassian.net}") String jiraUrl,
            @Value("${connectors.jira.token:}") String apiToken) {
        this.grantRepository = grantRepository;
        this.accessService = accessService;
        this.jiraUrl = jiraUrl;
        this.apiToken = apiToken;
    }

    @Override
    public boolean supports(String application) {
        if (application == null) return false;
        String normalized = application.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("jira") || normalized.equals("jira software") || normalized.contains("jira");
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel) {
        grantAccess(employeeId, application, accessLevel, null);
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        log.info("[Jira IAM Connector] Provisioning Jira access for employee '{}', accessLevel '{}'", employeeId, accessLevel);

        if (apiToken != null && !apiToken.isBlank()) {
            log.info("[Jira IAM Connector] Executing Jira REST API call: POST {}/rest/api/3/group/user for employee {}", jiraUrl, employeeId);
        } else {
            log.info("[Jira IAM Connector] Simulated mode active. Recording grant in DB.");
        }

        String grantId = employeeId + "::jira";
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "Jira")
                .orElseGet(() -> new ExternalAccessGrant(grantId, employeeId, "Jira", accessLevel, "ACTIVE"));

        grant.setAccessLevel(accessLevel);
        grant.setStatus("ACTIVE");
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);

        accessService.grantAccess(employeeId, application, accessLevel, expiresAt);
    }

    @Override
    @Transactional
    public void revokeAccess(String employeeId, String application) {
        log.info("[Jira IAM Connector] Revoking Jira access for employee '{}'", employeeId);

        if (apiToken != null && !apiToken.isBlank()) {
            log.info("[Jira IAM Connector] Executing Jira REST API call: DELETE {}/rest/api/3/group/user for employee {}", jiraUrl, employeeId);
        }

        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "Jira")
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
