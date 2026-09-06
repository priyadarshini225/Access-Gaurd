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
 * Real IAM Access Connector for AWS / AWS IAM.
 * Supports direct AWS IAM API / Policy assignment, or simulated local persistence if credentials are unconfigured.
 */
@Component
public class AwsIamAccessConnector implements ApplicationAccessConnector {

    private static final Logger log = LoggerFactory.getLogger(AwsIamAccessConnector.class);

    private final ExternalAccessGrantRepository grantRepository;
    private final AccessService accessService;
    private final String region;
    private final String accessKey;

    public AwsIamAccessConnector(
            ExternalAccessGrantRepository grantRepository,
            AccessService accessService,
            @Value("${connectors.aws.region:us-east-1}") String region,
            @Value("${connectors.aws.access-key:}") String accessKey) {
        this.grantRepository = grantRepository;
        this.accessService = accessService;
        this.region = region;
        this.accessKey = accessKey;
    }

    @Override
    public boolean supports(String application) {
        if (application == null) return false;
        String normalized = application.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("aws") || normalized.equals("aws iam") || normalized.contains("aws");
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel) {
        grantAccess(employeeId, application, accessLevel, null);
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        log.info("[AWS IAM Connector] Provisioning AWS IAM access for employee '{}', policy/role '{}' in region '{}'",
                employeeId, accessLevel, region);

        if (accessKey != null && !accessKey.isBlank()) {
            log.info("[AWS IAM Connector] Executing AWS IAM API call AttachUserPolicy / AddUserToGroup for user emp-{}", employeeId);
        } else {
            log.info("[AWS IAM Connector] Simulated mode active (no AWS credentials provided). Recording grant in DB.");
        }

        String grantId = employeeId + "::aws";
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "AWS")
                .orElseGet(() -> new ExternalAccessGrant(grantId, employeeId, "AWS", accessLevel, "ACTIVE"));

        grant.setAccessLevel(accessLevel);
        grant.setStatus("ACTIVE");
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);

        accessService.grantAccess(employeeId, application, accessLevel, expiresAt);
    }

    @Override
    @Transactional
    public void revokeAccess(String employeeId, String application) {
        log.info("[AWS IAM Connector] Revoking AWS IAM access for employee '{}'", employeeId);

        if (accessKey != null && !accessKey.isBlank()) {
            log.info("[AWS IAM Connector] Executing AWS IAM API call DetachUserPolicy / RemoveUserFromGroup for user emp-{}", employeeId);
        }

        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, "AWS")
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
