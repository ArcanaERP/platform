package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteSummaryView(
    String tenantCode,
    LocalDate businessDate,
    long noteCount,
    long invoiceCount
) {
}
