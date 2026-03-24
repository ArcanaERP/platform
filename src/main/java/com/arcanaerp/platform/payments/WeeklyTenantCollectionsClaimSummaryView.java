package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsClaimSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
