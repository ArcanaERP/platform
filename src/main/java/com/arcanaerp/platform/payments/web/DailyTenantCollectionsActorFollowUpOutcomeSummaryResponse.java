package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsActorFollowUpOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String changedBy,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
