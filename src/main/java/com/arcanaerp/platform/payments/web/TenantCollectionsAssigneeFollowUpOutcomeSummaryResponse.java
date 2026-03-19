package com.arcanaerp.platform.payments.web;

public record TenantCollectionsAssigneeFollowUpOutcomeSummaryResponse(
    String tenantCode,
    String assignedTo,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
