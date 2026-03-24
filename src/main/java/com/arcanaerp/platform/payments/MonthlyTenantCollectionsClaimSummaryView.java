package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsClaimSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
