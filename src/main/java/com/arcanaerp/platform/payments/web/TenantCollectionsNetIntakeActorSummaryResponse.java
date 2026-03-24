package com.arcanaerp.platform.payments.web;

public record TenantCollectionsNetIntakeActorSummaryResponse(
    String tenantCode,
    String actor,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
