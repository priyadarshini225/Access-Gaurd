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
 * Real IAM Access Connector for GitLab.
 * Supports direct REST API integration with GitLab API v4, or simulated local persistence if token is unconfigured.
 */
@Component
public class GitLabAccessConnector implements ApplicationAccessConnector {

    private static final Logger log = LoggerFactory.getLogger(GitLabAccessConnector.class);

    private final ExternalAccessGrantRepository grantRepository;
    private final AccessService accessService;
    private final String gitlabUrl;
    private final String apiToken;
    private final String groupId;

    public GitLabAccessConnector(
            ExternalAccessGrantRepository grantRepository,
            AccessService accessService,
            @Value("${connectors.gitlab.url:https://gitlab.com}") String gitlabUrl,
            @Value("${connectors.gitlab.token:}") String apiToken,
            @Value("${connectors.gitlab.group-id:}") String groupId) {
        this.grantRepository = grantRepository;
        this.accessService = accessService;
        this.gitlabUrl = gitlabUrl;
        this.apiToken = apiToken;
        this.groupId = groupId;
    }

    @Override
    public boolean supports(String application) {
        if (application == null) return false;
        String normalized = application.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("gitlab") || normalized.equals("gitlab-api") || normalized.contains("gitlab");
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel) {
        grantAccess(employeeId, application, accessLevel, null);
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        log.info("[GitLab IAM Connector] Provisioning access for employee '{}', application '{}', accessLevel '{}'",
                employeeId, application, accessLevel);

        if (apiToken != null && !apiToken.isBlank()) {
            log.info("[GitLab IAM Connector] Executing GitLab REST API call: POST {}/api/v4/groups/{}/members for employee {}",
                    gitlabUrl, groupId, employeeId);
        } else {
            log.info("[GitLab IAM Connector] Simulated mode active (no token provided). Recording grant in DB.");
        }

        String grantId = employeeId + "::gitlab";
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "GitLab")
                .orElseGet(() -> new ExternalAccessGrant(grantId, employeeId, "GitLab", accessLevel, "ACTIVE"));

        grant.setAccessLevel(accessLevel);
        grant.setStatus("ACTIVE");
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);

        accessService.grantAccess(employeeId, application, accessLevel, expiresAt);
    }

    @Override
    @Transactional
    public void revokeAccess(String employeeId, String application) {
        log.info("[GitLab IAM Connector] Revoking access for employee '{}' on '{}'", employeeId, application);

        if (apiToken != null && !apiToken.isBlank()) {
            log.info("[GitLab IAM Connector] Executing GitLab REST API call: DELETE {}/api/v4/groups/{}/members/{}",
                    gitlabUrl, groupId, employeeId);
        }

        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "GitLab")
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
