package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsNetIntakeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
