package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNetIntakeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
