package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsReleaseSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
