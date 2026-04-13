package com.arcanaerp.platform.identity;

import java.time.Instant;
import java.util.UUID;

public record TenantView(
    UUID id,
    String code,
    String name,
    Instant createdAt
) {
}
