package org.example.Entities;

public record EmailRequest(String to, String subject, String htmlTemplate, Priority priority) {
    public enum Priority { HIGH, MEDIUM, LOW }
}
