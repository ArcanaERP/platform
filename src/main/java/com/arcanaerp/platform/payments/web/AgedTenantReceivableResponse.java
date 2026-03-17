package com.arcanaerp.platform.payments.web;

import com.arcanaerp.platform.payments.ReceivablesAgingBucket;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AgedTenantReceivableResponse(
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
    Instant followUpSetAt
) {
}
