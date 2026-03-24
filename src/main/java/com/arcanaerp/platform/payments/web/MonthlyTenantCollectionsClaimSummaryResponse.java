package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsClaimSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
