package compensation_engine.connector;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationAccessConnectorRegistry {

    private final List<ApplicationAccessConnector> connectors;

    public ApplicationAccessConnectorRegistry(List<ApplicationAccessConnector> connectors) {
        this.connectors = connectors;
    }

    public ApplicationAccessConnector resolve(String application) {
        if (application == null || application.isBlank()) {
            throw new IllegalArgumentException("Application is required to resolve an access connector.");
        }

        return connectors.stream()
            .filter(connector -> !connector.isFallback())
            .filter(connector -> connector.supports(application.trim()))
            .findFirst()
            .orElseGet(() -> connectors.stream()
                .filter(ApplicationAccessConnector::isFallback)
                .filter(connector -> connector.supports(application.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "No access connector is registered for application: " + application)));
    }
}
