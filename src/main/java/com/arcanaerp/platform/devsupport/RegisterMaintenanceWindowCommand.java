package com.arcanaerp.platform.devsupport;

import java.time.Instant;

public record RegisterMaintenanceWindowCommand(
    String tenantCode,
    String windowCode,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    boolean active
) {
}
