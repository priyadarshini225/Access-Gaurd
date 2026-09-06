package compensation_engine.connector;

import compensation_engine.exception.DataInconsistencyException;
import compensation_engine.model.ExternalAccessGrant;
import compensation_engine.repository.ExternalAccessGrantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class ConfiguredExternalAccessConnector implements ApplicationAccessConnector {

    private final ExternalAccessGrantRepository grantRepository;
    private final Set<String> configuredApplications;

    public ConfiguredExternalAccessConnector(
            ExternalAccessGrantRepository grantRepository,
            @Value("${connectors.external.applications:}") String configuredApplications) {
        this.grantRepository = grantRepository;
        this.configuredApplications = parseApplications(configuredApplications);
    }

    @Override
    public boolean supports(String application) {
        return application != null
                && configuredApplications.contains(normalize(application));
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel) {
        grantAccess(employeeId, application, accessLevel, null);
    }

    @Override
    @Transactional
    public void grantAccess(String employeeId, String application, String accessLevel, Instant expiresAt) {
        String grantId = grantId(employeeId, application);
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, application)
                .orElseGet(() -> new ExternalAccessGrant(
                        grantId, employeeId, application, accessLevel, "ACTIVE"));
        grant.setAccessLevel(accessLevel);
        grant.setStatus("ACTIVE");
        grant.setExpiresAt(expiresAt);
        grantRepository.save(grant);
    }

    @Override
    @Transactional
    public void revokeAccess(String employeeId, String application) {
        ExternalAccessGrant grant = grantRepository
                .findByEmployeeIdAndApplicationIgnoreCase(employeeId, application)
                .orElseThrow(() -> new DataInconsistencyException(
                        "ExternalAccess(" + application + ")", employeeId));
        grantRepository.delete(grant);
    }

    @Override
    @Transactional
    public void revokeAllAccess(String employeeId) {
        grantRepository.deleteByEmployeeId(employeeId);
    }

    private Set<String> parseApplications(String configuredApplications) {
        if (configuredApplications == null || configuredApplications.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredApplications.split(","))
                .map(this::normalize)
                .filter(application -> !application.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String grantId(String employeeId, String application) {
        return employeeId + "::" + normalize(application);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
