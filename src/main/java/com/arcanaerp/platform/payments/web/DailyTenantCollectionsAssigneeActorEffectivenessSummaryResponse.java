package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailyTenantCollectionsAssigneeActorEffectivenessSummaryResponse(
    String tenantCode,
    String currencyCode,
    LocalDate businessDate,
    String assignedTo,
    String changedBy,
    long currentAssignedInvoiceCount,
    BigDecimal currentOutstandingAmount,
    Instant oldestDueAt,
    long completionCount,
    long completedInvoiceCount
) {
}
