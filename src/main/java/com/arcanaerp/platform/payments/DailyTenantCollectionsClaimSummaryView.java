package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsClaimSummaryView(
    String tenantCode,
    LocalDate businessDate,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
