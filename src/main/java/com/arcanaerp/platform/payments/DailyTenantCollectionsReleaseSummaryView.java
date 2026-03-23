package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsReleaseSummaryView(
    String tenantCode,
    LocalDate businessDate,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
