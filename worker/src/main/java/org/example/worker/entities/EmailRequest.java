package org.example.worker.entities;

public record EmailRequest(String to, String subject, String htmlTemplate, Priority priority) {
    public enum Priority { HIGH, MEDIUM, LOW }
}
