package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AgedTenantReceivableView(
    String tenantCode,
    String currencyCode,
    String invoiceNumber,
    Instant dueAt,
    Instant issuedAt,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    LocalDate asOfDate,
    long daysPastDue,
    ReceivablesAgingBucket agingBucket,
    String assignedTo,
    String assignedBy,
    Instant assignedAt,
    Instant followUpAt,
    String followUpSetBy,
    Instant followUpSetAt,
    CollectionsFollowUpOutcome latestFollowUpOutcome
) {
}
