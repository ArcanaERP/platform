package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeeklyTenantCollectionsAssigneeActorEffectivenessSummaryView(
    String tenantCode,
    String currencyCode,
    LocalDate businessWeekStart,
    String assignedTo,
    String changedBy,
    long currentAssignedInvoiceCount,
    BigDecimal currentOutstandingAmount,
    Instant oldestDueAt,
    long completionCount,
    long completedInvoiceCount
) {
}
