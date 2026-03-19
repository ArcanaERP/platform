package com.arcanaerp.platform.payments;

public record TenantCollectionsAssigneeFollowUpOutcomeSummaryView(
    String tenantCode,
    String assignedTo,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
