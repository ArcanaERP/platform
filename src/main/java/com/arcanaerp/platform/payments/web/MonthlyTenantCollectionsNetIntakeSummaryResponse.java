package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNetIntakeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
