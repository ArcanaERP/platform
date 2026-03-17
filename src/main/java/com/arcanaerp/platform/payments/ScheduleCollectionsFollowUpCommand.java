package com.arcanaerp.platform.payments;

import java.time.Instant;

public record ScheduleCollectionsFollowUpCommand(
    String tenantCode,
    String invoiceNumber,
    Instant followUpAt,
    String scheduledBy
) {
}
