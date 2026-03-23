package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsReleaseSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
