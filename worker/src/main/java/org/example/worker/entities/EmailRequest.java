package org.example.worker.entities;

public record EmailRequest(String to, String subject, String htmlTemplate) {}
