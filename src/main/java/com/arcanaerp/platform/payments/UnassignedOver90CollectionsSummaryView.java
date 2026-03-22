package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record UnassignedOver90CollectionsSummaryView(
    String tenantCode,
    String currencyCode,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
