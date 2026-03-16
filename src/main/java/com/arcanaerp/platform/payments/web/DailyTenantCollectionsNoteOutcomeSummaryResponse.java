package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
