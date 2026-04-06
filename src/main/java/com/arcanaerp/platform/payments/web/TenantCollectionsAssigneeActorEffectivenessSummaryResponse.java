package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeActorEffectivenessSummaryResponse(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    String changedBy,
    long currentAssignedInvoiceCount,
    BigDecimal currentOutstandingAmount,
    Instant oldestDueAt,
    long completionCount,
    long completedInvoiceCount
) {
}
