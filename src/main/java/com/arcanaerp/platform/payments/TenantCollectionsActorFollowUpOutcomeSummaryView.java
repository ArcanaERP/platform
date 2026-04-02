package com.arcanaerp.platform.payments;

public record TenantCollectionsActorFollowUpOutcomeSummaryView(
    String tenantCode,
    String changedBy,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
