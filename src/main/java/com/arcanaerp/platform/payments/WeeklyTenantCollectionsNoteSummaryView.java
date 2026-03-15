package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    long noteCount,
    long invoiceCount
) {
}
