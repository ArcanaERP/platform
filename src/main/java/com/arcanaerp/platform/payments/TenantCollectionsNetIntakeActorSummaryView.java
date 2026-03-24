package com.arcanaerp.platform.payments;

public record TenantCollectionsNetIntakeActorSummaryView(
    String tenantCode,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
