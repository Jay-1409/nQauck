package org.example.Security;

import java.time.Instant;

public record AccessCoin(
        String id,
        String name,
        String token,
        Instant createdAt,
        boolean revoked
) {}
