package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    long noteCount,
    long invoiceCount
) {
}
