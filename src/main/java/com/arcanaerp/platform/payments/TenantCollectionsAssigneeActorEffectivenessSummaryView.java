package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeActorEffectivenessSummaryView(
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
