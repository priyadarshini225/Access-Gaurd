package compensation_engine.connector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class ApplicationAccessConnectorRegistryTest {

    @Test
    void specificConnectorTakesPrecedenceOverFallback() {
        ApplicationAccessConnector fallback = new TestConnector(true, "fallback");
        ApplicationAccessConnector gitLab = new TestConnector(false, "gitlab") {
            @Override
            public boolean supports(String application) {
                return "gitlab".equalsIgnoreCase(application);
            }
        };

        ApplicationAccessConnectorRegistry registry =
                new ApplicationAccessConnectorRegistry(List.of(fallback, gitLab));

        assertSame(gitLab, registry.resolve("GitLab"));
    }

    @Test
    void fallbackConnectorHandlesUnregisteredApplications() {
        ApplicationAccessConnector fallback = new TestConnector(true, "fallback");
        ApplicationAccessConnectorRegistry registry =
                new ApplicationAccessConnectorRegistry(List.of(fallback));

        assertSame(fallback, registry.resolve("Jira"));
    }

    private static class TestConnector implements ApplicationAccessConnector {

        private final boolean fallback;
        private final String name;

        private TestConnector(boolean fallback, String name) {
            this.fallback = fallback;
            this.name = name;
        }

        @Override
        public boolean supports(String application) {
            return true;
        }

        @Override
        public boolean isFallback() {
            return fallback;
        }

        @Override
        public void grantAccess(String employeeId, String application, String accessLevel) {}

        @Override
        public void revokeAccess(String employeeId, String application) {}

        @Override
        public void revokeAllAccess(String employeeId) {}

        @Override
        public String toString() {
            return name;
        }
    }
}
