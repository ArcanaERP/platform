package com.arcanaerp.platform.payments.web;

public record TenantCollectionsActorFollowUpOutcomeSummaryResponse(
    String tenantCode,
    String changedBy,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
