package org.example.configuration;

import org.example.Security.DashboardDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardConfigurationServiceTest {

    @Test
    void writesPublisherYamlWithoutSmtpCredentials(@TempDir Path directory) throws Exception {
        DashboardDataStore dataStore = mock(DashboardDataStore.class);
        when(dataStore.loadConfiguration()).thenReturn(Optional.empty());
        DashboardConfigurationService service = new DashboardConfigurationService(dataStore,
                new MockEnvironment().withProperty("app.dashboard.data-dir", directory.toString()));

        service.generate(new DashboardConfiguration(QueueProvider.KAFKA, "smtp.example.com", 587,
                "from@example.com", true, true, "smtp-user", "smtp-secret", false,
                "localhost", 5672, "localhost:9092"));

        String publisherYaml = Files.readString(directory.resolve("publisher-application.yml"));
        assertTrue(publisherYaml.contains("provider: KAFKA"));
        assertTrue(publisherYaml.contains("localhost:9092"));
        assertFalse(publisherYaml.contains("smtp-secret"));
    }
}
