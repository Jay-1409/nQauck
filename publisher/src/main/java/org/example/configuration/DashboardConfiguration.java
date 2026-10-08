package org.example.configuration;

public record DashboardConfiguration(
        QueueProvider provider,
        String smtpHost,
        int smtpPort,
        String fromEmail,
        boolean smtpAuth,
        boolean smtpStarttls,
        String smtpUsername,
        String smtpPassword,
        boolean priorityScheduling,
        String prioritySequence) {}
