package org.example.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardConfiguration(
        QueueProvider provider,
        String smtpHost,
        int smtpPort,
        String fromEmail,
        boolean smtpAuth,
        boolean smtpStarttls,
        String smtpUsername,
        String smtpPassword,
        boolean priorityScheduling) {}
