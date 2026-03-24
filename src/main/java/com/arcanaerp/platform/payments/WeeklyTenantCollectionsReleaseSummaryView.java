package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsReleaseSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
