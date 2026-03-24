package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsClaimSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
