package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsNetIntakeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
