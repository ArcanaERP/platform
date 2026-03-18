package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsFollowUpOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
