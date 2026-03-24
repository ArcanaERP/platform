package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNetIntakeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
