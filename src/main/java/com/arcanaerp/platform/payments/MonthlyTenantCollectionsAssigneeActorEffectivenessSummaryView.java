package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

public record MonthlyTenantCollectionsAssigneeActorEffectivenessSummaryView(
    String tenantCode,
    String currencyCode,
    YearMonth businessMonth,
    String assignedTo,
    String changedBy,
    long currentAssignedInvoiceCount,
    BigDecimal currentOutstandingAmount,
    Instant oldestDueAt,
    long completionCount,
    long completedInvoiceCount
) {
}
