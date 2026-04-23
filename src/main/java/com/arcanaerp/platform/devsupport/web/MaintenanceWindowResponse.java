package com.arcanaerp.platform.devsupport.web;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceWindowResponse(
    UUID id,
    String tenantCode,
    String windowCode,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    boolean active,
    Instant createdAt
) {
}
