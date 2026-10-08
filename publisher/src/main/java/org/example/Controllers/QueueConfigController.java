package org.example.Controllers;

import org.example.configuration.DashboardConfiguration;
import org.example.configuration.DashboardConfigurationService;
import org.example.configuration.DashboardConfigurationService.InvalidConfigurationException;
import org.example.configuration.DashboardConfigurationService.SavedConfiguration;
import org.example.configuration.QueueProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class QueueConfigController {

    private final DashboardConfigurationService configurationService;

    public QueueConfigController(DashboardConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrfToken(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping(value = "/config.yml", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SavedConfiguration> savedConfiguration() {
        return configurationService.load()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/config.yml", produces = "application/yaml")
    public ResponseEntity<String> generate(
            @RequestParam QueueProvider provider,
            @RequestParam String smtpHost,
            @RequestParam int smtpPort,
            @RequestParam String fromEmail,
            @RequestParam(defaultValue = "false") boolean smtpAuth,
            @RequestParam(defaultValue = "false") boolean smtpStarttls,
            @RequestParam(defaultValue = "") String smtpUsername,
            @RequestParam(defaultValue = "") String smtpPassword,
            @RequestParam(defaultValue = "false") boolean priorityScheduling,
            @RequestParam(defaultValue = "") String prioritySequence) {
        String yaml = configurationService.generate(new DashboardConfiguration(provider, smtpHost, smtpPort,
                fromEmail, smtpAuth, smtpStarttls, smtpUsername, smtpPassword, priorityScheduling, prioritySequence));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=application.yml")
                .contentType(MediaType.parseMediaType("application/yaml"))
                .body(yaml);
    }

    @ExceptionHandler(InvalidConfigurationException.class)
    public ResponseEntity<String> invalidConfiguration(InvalidConfigurationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
