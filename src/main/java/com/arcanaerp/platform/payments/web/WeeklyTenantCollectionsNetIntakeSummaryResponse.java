package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNetIntakeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
