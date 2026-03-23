package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsClaimSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String claimedBy,
    long claimCount,
    long invoiceCount
) {
}
